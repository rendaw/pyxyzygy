#include "header.hxx"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <string>
#include <cassert>
#include <cerrno>
#include <memory>

#include <png.h>

#include "gzutil.hxx"

static int const channels = 4;
static int16_t const paletteVersion = 4;

static inline int32_t posCeilDiv(int32_t const x, int32_t const y) {
	return (x + y - 1) / y;
}

template <class T> static inline T msq(T const &a) {
	return a * a;
}

static l_t scratchSize = 0;
static l_t scratchBitWidth = 0;
static l_t scratchHeight = 0;
static l_t scratchByteWidth;
static uint8_t *scratch = nullptr;

static void clearScratch() {
	memset(scratch, 0, scratchHeight * scratchByteWidth);
}

static void prepareScratchInternal() {
	c_t const needSize = posCeilDiv(scratchHeight * scratchByteWidth, 4);
	if (scratch == nullptr || scratchSize < needSize) {
		scratchSize = needSize;
		delete [] scratch;
		scratch = (uint8_t *)new c_t[scratchSize];
	}
	clearScratch();
}

static void prepareBitScratch(l_t const width, l_t const height) {
	scratchBitWidth = width;
	scratchByteWidth = posCeilDiv(width, 8);
	scratchHeight = height;
	prepareScratchInternal();
}

static void prepareByteScratch(l_t const width, l_t const height) {
	scratchByteWidth = width;
	scratchBitWidth = width * 8;
	scratchHeight = height;
	prepareScratchInternal();
}

static uint8_t getBitScratch(l_t x, l_t y) {
	if (y >= scratchHeight) {
		return 0;
	}
	if (x >= scratchBitWidth) {
		return 0;
	}
	l_t bit = x % 8;
	x = x / 8;
	return (scratch[y * scratchByteWidth + x] >> bit) & 1u;
}

static void setBitScratch(l_t x, l_t y) {
	assert(x < scratchBitWidth);
	assert(y < scratchHeight);
	//printf("\tp %u %u\n", x, y);
	l_t bit = x % 8;
	x = x / 8;
	scratch[y * scratchByteWidth + x] |= 1u << bit;
	//debugScratch();
	//debugRawScratch();
}

static void setBitScratchLine(l_t y, l_t start, l_t end) {
	//printf("fl y %u, %u - %u\n", y, start, end);
	assert(end <= scratchBitWidth);
	assert(y < scratchHeight);
	l_t alignedStart = posCeilDiv(start, 8);
	l_t alignedEnd = std::min(scratchByteWidth, end / 8);
	//printf("\talign start %u end %u\n", alignedStart, alignedEnd);
	if (alignedEnd < alignedStart) {
		for (l_t x = start; x < end; ++x) {
			setBitScratch(x, y);
		}
	} else {
		for (l_t x = start; x < alignedStart * 8; ++x) {
			setBitScratch(x, y);
		}
		memset(&scratch[y * scratchByteWidth + alignedStart], 0xff, alignedEnd - alignedStart);
		for (l_t x = alignedEnd * 8; x < end; ++x) {
			setBitScratch(x, y);
		}
	}
}

static inline void premultiply(uint8_t *dest, uint8_t const * const source) {
	int32_t const alpha = source[3];
	for (int i = 0; i < channels - 1; ++i)
		dest[i] = (source[i] * alpha) >> 8u;
	dest[3] = source[3];
}

static inline void tint(uint8_t *dest, uint8_t const * const source, c_t const *colors) {
	for (int i = 0; i < channels - 1; ++i)
		dest[i] = ((source[i] + colors[i]) * 0x80u) >> 8u;
	dest[3] = source[3];
}

ROBytes::ROBytes(size_t const size, uint8_t const * const data) : size(size), data(data) {
}

static const size_t palsize = sizeof(p_t);

void PaletteColors::set(p_t index, uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca) {
	uint32_t const color = htonl(
		((uint32_t)cb << 24) |
		((uint32_t)cg << 16) |
		((uint32_t)cr << 8) |
		(uint32_t)ca
	);
	for (int i = 0; i < colors.size(); ++i) {
		auto &p = colors[i];
		if (p.first != index) continue;
		if (color == 0) {
			colors.erase(colors.begin() + i);
			return;
		} else {
			p.second = color;
			return;
		}
	}
	if (color == 0) return;
	colors.emplace_back(index, color);
}

c_t PaletteColors::get(p_t index) const {
	for (auto const &p : colors) {
		if (p.first == index) return p.second;
	}
	return (c_t)0;
}

template<class HandleMeasures, class SetPixelLine> static void strokeSolid(
	double const x1_0, double const y1_0, double const r1_0,
   	double const x2_0, double const y2_0, double const r2_0,
	int const subpixels,
	/*
	 * offsetX/Y are the offset of the subpixel rendering region to the original (image) coordinate system.
	 * The subpixel region is a space scaled by subpixels but aligned to a multiple of subpixels
	 * So the subpixel left/top coords are always in [0,subpixels)
	 *
	 * In the case that subpixels == 1, subpixel top/left will be 0
	 */
   	HandleMeasures &&handleMeasures,
	/*
	 * y, start, stop are coordinates in subpixel space.
	 * If subpixels == 1, this will be image space shifted by the offset (actual coord = y/start/end + offsetX/offsetY)
	 */
	SetPixelLine &&setPixelLine
) {
	struct FillEdge {
		virtual std::pair<float, float> getX(int y) const = 0;
	};

	auto const fill = [&](float const yTop, float const yBottom, FillEdge const &edge1, FillEdge const &edge2) {
		int const intTop = std::round(yTop);
		int const intBottom = std::round(yBottom);
		if (intTop == intBottom) {
			return;
		}
		//printf("fr y %f - %f = %d - %d\n", yTop, yBottom, intTop, intBottom);

		auto const x1 = edge1.getX(intTop);
		auto const x2 = edge2.getX(intTop);
		//printf("x1 %f %f x2 %f %f\n", x1.first, x1.second, x2.first, x2.second);
		FillEdge const *left, *right;
		if (x1.first < x2.first) {
			left = &edge1;
			right = &edge2;
			setPixelLine(intTop, x1.first, x2.second);
		} else {
			left = &edge2;
			right = &edge1;
			setPixelLine(intTop, x2.first, x1.second);
		}
		for (int y = intTop + 1; y < intBottom; ++y) {
			setPixelLine(y, left->getX(y).first, right->getX(y).second);
		}
	};

	struct Vector {
		float const x;
		float const y;

		Vector(float x, float y) : x(x), y(y) {}

		Vector plus(Vector const &other) const {
			return Vector(x + other.x, y + other.y);
		}

		Vector mult(float const factor) const {
			return Vector(x * factor, y * factor);
		}
	};

	struct Circle : FillEdge {
		Vector const center;
		float const radius;
		float const left;
		float const right;
		float const top;
		float const bottom;

		Circle(Vector const &center, float const radius) :
			center(center), radius(radius), left(center.x - radius), right(center.x + radius), top(center.y - radius), bottom(center.y + radius) {
		}

		Circle adjust(int offsetX, int offsetY, int subpixels) const {
			return Circle{
				Vector{
					(center.x - offsetX) * subpixels,
					(center.y - offsetY) * subpixels,
				},
				radius * subpixels
			};
		}

		std::pair<float, float> getX(int y) const override {
			float const x = std::sqrt(msq(radius) - msq((y + 0.5) - center.y));
			float const start = center.x - x;
			float const end = center.x + x;
			//printf("circ y: %d, x: %f %f\n", y, start, end);
			return {start, end};
		}
	};

	int offsetX;
	int offsetY;
	int subpixelTop;
	int subpixelBottom;
	int subpixelLeft;
	int subpixelRight;

	float const sqDistX = msq(x1_0 - x2_0);
	float const sqDistY = msq(y1_0 - y2_0);
	float const sqDist = sqDistX + sqDistY;
	if (sqDist <= msq(r1_0 - r2_0)) {
		//printf("concentric! sqdist %f = %f + %f, rsq %f\n", sqDist, sqDistX, sqDistY, msq(r1_0 - r2_0));
		// Concentric
		Circle circle = [&]() {
		   	return r1_0 > r2_0 ? Circle(Vector(x1_0, y1_0), r1_0) : Circle(Vector(x2_0, y2_0), r2_0);
	   	}();
		//printf("orig circle: %f %f %f / tblr %f %f %f %f\n", circle.center.x, circle.center.y, circle.radius, circle.top, circle.bottom, circle.left, circle.right);
		offsetX = circle.left;
		offsetY = circle.top;
		auto subpixelCircle = circle.adjust(offsetX, offsetY, subpixels);
		//printf("subpixel circle: %f %f %f / tblr %f %f %f %f\n", subpixelCircle.center.x, subpixelCircle.center.y, subpixelCircle.radius, subpixelCircle.top, subpixelCircle.bottom, subpixelCircle.left, subpixelCircle.right);
		subpixelTop = subpixelCircle.top;
		subpixelBottom = std::ceil(subpixelCircle.bottom);
		subpixelLeft = subpixelCircle.left;
		subpixelRight = std::ceil(subpixelCircle.right);
		handleMeasures(offsetX, offsetY, subpixelTop, subpixelBottom, subpixelLeft, subpixelRight);
		fill(subpixelCircle.top, subpixelCircle.bottom, subpixelCircle, subpixelCircle);
	} else {
		// Not concentric - capsule shape

		// Invariants and laws
		// tangent segment lengths are equal
		// coords within tangent top point pair and bottom point pair will have same order
		// tangent points are always away from the circle bisection in the direction the line tilts

		struct Line {
			bool const horizontal;
		};
		struct Segment : FillEdge {
			Vector const pTop;
			Vector const pBottom;
			float const xDiff;
			float const yDiff;

			Segment(Vector const &pTop, Vector const &pBottom) :
			   	pTop(pTop), pBottom(pBottom), xDiff(pBottom.x - pTop.x), yDiff(pBottom.y - pTop.y) {}

			std::pair<float, float> getX(int y) const override {
				float const x = pTop.x + ((y + 0.5) - pTop.y) * xDiff / yDiff;
				//printf("seg y: %d, x: %f\n", y, x);
				return {x, x};
			}
		};
		auto const createSegment = [](Vector const &p1, Vector const &p2) -> Segment {
			if (p1.y < p2.y) {
				return {p1, p2};
			}
			return {p2, p1};
		};

		Vector const p1(x1_0, y1_0);
		Vector const p2(x2_0, y2_0);

		// define circles
		Circle const circle1(p1, r1_0);
		Circle const circle2(p2, r2_0);
		//printf("orig circle 1: %f %f %f / tblr %f %f %f %f\n", circle1.center.x, circle1.center.y, circle1.radius, circle1.top, circle1.bottom, circle1.left, circle1.right);
		//printf("orig circle 2: %f %f %f / tblr %f %f %f %f\n", circle2.center.x, circle2.center.y, circle2.radius, circle2.top, circle2.bottom, circle2.left, circle2.right);
		offsetX = std::min(circle1.left, circle2.left);
		offsetY = std::min(circle1.top, circle2.top);

		// Circles are now in offset subpixel coordinates
		// note: subpixel scratch might have up to subpixels of dead space at top left since I wanted to calculate offsets before the subpixel circles, to do adjustment all in one step
		auto const subpixelCircle1 = circle1.adjust(offsetX, offsetY, subpixels);
		auto const subpixelCircle2 = circle2.adjust(offsetX, offsetY, subpixels);
		struct _t0 { Circle const &t; Circle const &b; };
		_t0 const &_t0a = subpixelCircle1.top < subpixelCircle2.top ?
			_t0{ subpixelCircle1, subpixelCircle2 } :
			_t0{ subpixelCircle2, subpixelCircle1 };
		auto const &topCircle = _t0a.t;
		auto const &bottomCircle = _t0a.b;
		//printf("subpixel circle 1: %f %f %f / tblr %f %f %f %f\n", topCircle.center.x, topCircle.center.y, topCircle.radius, topCircle.top, topCircle.bottom, topCircle.left, topCircle.right);
		//printf("subpixel circle 2: %f %f %f / tblr %f %f %f %f\n", bottomCircle.center.x, bottomCircle.center.y, bottomCircle.radius, bottomCircle.top, bottomCircle.bottom, bottomCircle.left, bottomCircle.right);

		// v range = min/max circle center +- r1/r2
		subpixelTop = std::min(subpixelCircle1.top, subpixelCircle2.top);
		subpixelBottom = std::ceil(std::max(subpixelCircle1.bottom, subpixelCircle2.bottom));
		subpixelLeft = std::min(subpixelCircle1.left, subpixelCircle2.left);
		subpixelRight = std::ceil(std::max(subpixelCircle1.right, subpixelCircle2.right));
		handleMeasures(offsetX, offsetY, subpixelTop, subpixelBottom, subpixelLeft, subpixelRight);

		// calculate segments
		float const mainAngle = atan2(
			subpixelCircle2.center.y - subpixelCircle1.center.y,
		   	subpixelCircle2.center.x - subpixelCircle1.center.x);
		float const segAngle = asin((r1_0 - r2_0) / std::sqrt(sqDist));
		float const perpSeg1Angle = mainAngle + segAngle - M_PI / 2;
		float const perpSeg2Angle = mainAngle - segAngle + M_PI / 2;
		Vector const perpSeg1Unit(cos(perpSeg1Angle), sin(perpSeg1Angle));
		Vector const perpSeg2Unit(cos(perpSeg2Angle), sin(perpSeg2Angle));
		Segment const seg1(createSegment(
			subpixelCircle1.center.plus(perpSeg1Unit.mult(subpixelCircle1.radius)),
		   	subpixelCircle2.center.plus(perpSeg1Unit.mult(subpixelCircle2.radius))
		));
		Segment const seg2(createSegment(
			subpixelCircle1.center.plus(perpSeg2Unit.mult(subpixelCircle1.radius)),
			subpixelCircle2.center.plus(perpSeg2Unit.mult(subpixelCircle2.radius))
		));
		struct _t1 { Segment const &t; Segment const &b; };
		auto const &_t1a = seg1.pTop.y < seg2.pTop.y ?
			_t1{ seg1, seg2 } :
			_t1{ seg2, seg1 };
		auto const &topSegment = _t1a.t;
		auto const &bottomSegment = _t1a.b;
		//printf("top seg: %f %f %f %f\n", topSegment.pTop.x, topSegment.pTop.y, topSegment.pBottom.x, topSegment.pBottom.y);
		//printf("bot seg: %f %f %f %f\n", bottomSegment.pTop.x, bottomSegment.pTop.y, bottomSegment.pBottom.x, bottomSegment.pBottom.y);

		// for range - both seg1 y > seg2 y = seg1-circ, circ-circ, circ-seg1
		//	fill scratch pixels
		if (topCircle.bottom >= bottomCircle.bottom) {
			// Roughly horizontal - circles overlap eachother entirely
			fill(topCircle.top, topSegment.pTop.y, topCircle, topCircle);
			fill(topSegment.pTop.y, topSegment.pBottom.y, topCircle, topSegment);
			fill(topSegment.pBottom.y, bottomSegment.pTop.y, topCircle, bottomCircle);
			fill(bottomSegment.pTop.y, bottomSegment.pBottom.y, topCircle, bottomSegment);
			fill(bottomSegment.pBottom.y, topCircle.bottom, topCircle, topCircle);
		} else if (topSegment.pBottom.y < bottomSegment.pTop.y) {
			// Segments don't overlap - circ circ overlap in the middle
			fill(topCircle.top, topSegment.pTop.y, topCircle, topCircle);
			fill(topSegment.pTop.y, topSegment.pBottom.y, topCircle, topSegment);
			fill(topSegment.pBottom.y, bottomSegment.pTop.y, topCircle, bottomCircle);
			fill(bottomSegment.pTop.y, bottomSegment.pBottom.y, bottomSegment, bottomCircle);
			fill(bottomSegment.pBottom.y, bottomCircle.bottom, bottomCircle, bottomCircle);
		} else {
			// Roughly vertical - segments overlap eachother
			fill(topCircle.top, topSegment.pTop.y, topCircle, topCircle);
			fill(topSegment.pTop.y, bottomSegment.pTop.y, topCircle, topSegment);
			fill(bottomSegment.pTop.y, topSegment.pBottom.y, topSegment, bottomSegment);
			fill(topSegment.pBottom.y, bottomSegment.pBottom.y, bottomSegment, bottomCircle);
			fill(bottomSegment.pBottom.y, bottomCircle.bottom, bottomCircle, bottomCircle);
		}
	}
}

template<class SetPixel> static void strokeSolidHard(
	double const x1_0, double const y1_0, double const r1_0,
   	double const x2_0, double const y2_0, double const r2_0,
   	SetPixel &&setPixel
) {
	l_t offsetX;
	l_t offsetY;
	strokeSolid(
		x1_0, y1_0, r1_0, x2_0, y2_0, r2_0, 1,
		[&](int const offsetX_0, int const offsetY_0, int const top, int const bottom, int const left, int const right) {
			offsetX = offsetX_0;
			offsetY = offsetY_0;
		},
		[&](l_t y, float start0, float end0) {
			l_t start = std::max(0, (l_t)std::round(start0 + offsetX));
			l_t end = std::round(end0 + offsetX);
			y += offsetY;
			for (l_t x = start; x < end; ++x) {
				setPixel(y, x);
			}
		}
	);
}

struct Overlap1D {
	l_t const a; // Where overlap starts relative to a
	l_t const b; // Where overlap starts relative to b
	l_t const span;
};

static Overlap1D const calcOverlap(l_t startRelA, l_t aSpan, l_t bSpan) {
	// startRelA - where b span starts relative to start of a span
	if (startRelA < 0) {
		return Overlap1D{0, -startRelA, std::min(aSpan, bSpan + startRelA)};
	} else {
		return Overlap1D{startRelA, 0, std::min(aSpan - startRelA, bSpan)};
	}
}

template <class HandleLine> static inline void merge(
	uint8_t *dest, size_t const unitStride, c_t const d_w, c_t const d_h, uint8_t const * const source, c_t const s_w, c_t const s_h, int32_t x0, int32_t y0, HandleLine const &handleLine
) {
	auto x = ::calcOverlap(x0, d_w, s_w);
	auto y = ::calcOverlap(y0, d_h, s_h);
	for (l_t y1 = 0; y1 < y.span; ++y1) {
		handleLine(
			&dest[((y.a + y1) * d_w + x.a) * unitStride],
			&source[((y.b + y1) * s_w + x.b) * unitStride],
			x.span
		);
	}
}

static inline void replace(uint8_t *dest, size_t const unitStride, c_t const d_w, c_t const d_h, uint8_t const * const source, c_t const s_w, c_t const s_h, int32_t x0, int32_t y0) {
	::merge(
		dest, unitStride, d_w, d_h, source, s_w, s_h, x0, y0,
		[](uint8_t *dest, uint8_t const * const source, int span) {
			memcpy(dest, source, span * channels);
		}
	);
}

PaletteImage::PaletteImage(l_t w, l_t h, p_t * const pixels) :
   	w(w), h(h), pixels(pixels) {
}

PaletteImage::~PaletteImage() {
	delete [] pixels;
}

PaletteImage * PaletteImage::create(l_t w, l_t h) {
	auto out = new PaletteImage(w, h, new p_t[w * h]);
	out->clear();
	return out;
}

PaletteImage * PaletteImage::deserialize(char const * const path) throw(std::runtime_error) {
	GZ source(path, "rb");
	auto version = source.read<uint16_t>();
	l_t width = source.read<l_t>();
	l_t height = source.read<l_t>();
	if (width == 0 || height == 0) throw std::runtime_error("bad image size");
	p_t *data = source.reada<p_t>(width * height);
	return new PaletteImage(width, height, data);
}

PaletteImage * PaletteImage::copy(l_t x0, l_t y0, l_t w0, l_t h0) const {
	auto x = ::calcOverlap(x0, w, w0);
	auto y = ::calcOverlap(y0, h, h0);
	PaletteImage * out = PaletteImage::create(w0, h0);
	for (l_t y1 = 0; y1 < y.span; ++y1) {
		memcpy(
			&out->pixels[(y.b + y1) * out->w + x.b],
			&pixels[(y.a + y1) * w + x.a],
			x.span * sizeof(p_t)
		);
	}
	return out;
}

template <class T> inline ROBytes PaletteImage::calculateData(T calculate) const {
	prepareByteScratch(w * channels, h);
	for (l_t y = 0; y < h; ++y) {
		for (l_t x = 0; x < w; ++x) {
			p_t const source = pixels[y * w + x];
			uint8_t *dest = &scratch[(y * w + x) * channels];
			calculate(dest, source);
		}
	}
	return {(size_t)(scratchByteWidth * scratchHeight), scratch};
}

ROBytes PaletteImage::data(PaletteColors const &palette) const {
	return calculateData([&](uint8_t *dest, p_t const source) {
		p_t &dest1 = *((p_t *) dest);
		dest1 = palette.get(source);
	});
}

ROBytes PaletteImage::dataPremultiplied(PaletteColors const &palette) const {
	return calculateData([&](uint8_t *dest, p_t const source) {
		*((uint32_t *) dest) = palette.get(source);
		premultiply(dest, dest);
	});
}

ROBytes PaletteImage::dataTint(PaletteColors const &palette, uint8_t cr, uint8_t cg, uint8_t cb) const {
	c_t const colors[] {cb, cg, cr};
	return calculateData([&](uint8_t *dest, p_t const source) {
		*((uint32_t *) dest) = palette.get(source);
		tint(dest, dest, colors);
	});
}

ROBytes PaletteImage::dataPremultipliedTint(PaletteColors const &palette, uint8_t cr, uint8_t cg, uint8_t cb) const {
	c_t const colors[] {cb, cg, cr};
	return calculateData([&](uint8_t *dest, p_t const source) {
		*((uint32_t *) dest) = palette.get(source);
		tint(dest, dest, colors);
		premultiply(dest, dest);
	});
}

l_t PaletteImage::getWidth() const {
	return w;
}

l_t PaletteImage::getHeight() const {
	return h;
}

void PaletteImage::clear() {
	memset(pixels, 0, w * h * palsize);
}

void PaletteImage::clear(l_t x, l_t y, l_t w0, l_t h0) {
	assert(x >= 0);
	assert(y >= 0);
	assert(x + w0 <= w);
	assert(y + h0 <= h);
	for (l_t y1 = y; y1 < y + h0; ++y1) {
		memset(
			&pixels[y1 * w + x],
			0,
			w0 * palsize
		);
	}
}

void PaletteImage::serialize(const char *path) const throw(std::runtime_error) {
	GZ dest(path, "wb");
	dest.write(paletteVersion);
	dest.write(w);
	dest.write(h);
	dest.writea(pixels, w * h);
}

void PaletteImage::setPixel(p_t index, l_t x, l_t y) {
	pixels[y * w + x] = index;
}
		
p_t PaletteImage::getPixel(l_t x, l_t y) const {
	return pixels[y * w + x];
}

void PaletteImage::stroke(p_t index, double x1_0, double y1_0, double r1_0, double x2_0, double y2_0, double r2_0) {
	strokeSolidHard(
		x1_0, y1_0, r1_0, x2_0, y2_0, r2_0,
		[&](l_t y, l_t x) {
			assert(x >= 0);
			assert(x < w);
			pixels[y * w + x] = index;
		}
	);
}

void PaletteImage::mergeColor(p_t oldIndex, p_t newIndex) {
	for (l_t y = 0; y < h; ++y) {
		for (l_t x = 0; x < w; ++x) {
			p_t &source = pixels[y * w + x];
			if (source == oldIndex) source = newIndex;
		}
	}
}

void PaletteImage::replace(PaletteImage const & source, int32_t x, int32_t y) {
	::replace((uint8_t *)pixels, sizeof(p_t), w, h, (uint8_t const *)source.pixels, source.w, source.h, x, y);
}

TrueColorImage::TrueColorImage(l_t w, l_t h, uint8_t * const pixels) :
   	w(w), h(h), pixels(pixels) {
}

TrueColorImage::~TrueColorImage() {
	delete [] pixels;
}

TrueColorImage * TrueColorImage::create(l_t w, l_t h) {
	auto out = new TrueColorImage(w, h, (uint8_t *)new c_t[w * h]);
	out->clear();
	return out;
}

TrueColorImage * TrueColorImage::deserialize(char const * const path) throw(std::runtime_error) {
	png_image img;
	memset(&img, 0, sizeof(img));
	img.version = PNG_IMAGE_VERSION;
	if (!png_image_begin_read_from_file(&img, path)) {
		throw std::runtime_error(std::string(img.message));
	}
	img.format = PNG_FORMAT_BGRA;
	auto pixels = (uint8_t *)new c_t[PNG_IMAGE_SIZE(img) / 4];
	if (!png_image_finish_read(&img, nullptr, pixels, 0, nullptr)) {
		throw std::runtime_error(std::string(img.message));
	}
	return new TrueColorImage(img.width, img.height, pixels);
}

TrueColorImage * TrueColorImage::copy(l_t x0, l_t y0, l_t w0, l_t h0) const {
	auto x = ::calcOverlap(x0, w, w0);
	auto y = ::calcOverlap(y0, h, h0);
	TrueColorImage * out = TrueColorImage::create(w0, h0);
	for (l_t y1 = 0; y1 < y.span; ++y1) {
		memcpy(
			&out->pixels[((y.b + y1) * out->w + x.b) * channels],
			&pixels[((y.a + y1) * w + x.a) * channels],
			x.span * channels
		);
	}
	return out;
}

TrueColorImage * TrueColorImage::scale(int scale) const {
	TrueColorImage * out = TrueColorImage::create(w * scale, h * scale);
	for (l_t y0 = 0; y0 < h; ++y0) {
		size_t const stride = w * scale * channels;
		for (l_t x0 = 0; x0 < w; ++x0) {
			uint32_t const pixel = *((uint32_t *)&pixels[(y0 * w + x0) * channels]);
			for (l_t x1 = 0; x1 < scale; ++x1) {
				*((uint32_t *)&out->pixels[
					((y0 * scale * w + x0) * scale + x1) * channels
				]) = pixel;
			}
		}
		for (l_t y1 = 1; y1 < scale; ++y1) {
			memcpy(
				&out->pixels[(y0 * scale + y1) * stride],
				&out->pixels[(y0 * scale) * stride],
				stride
			);
		}
	}
	return out;
}

template <class T> inline ROBytes TrueColorImage::calculateData(T calculate) const {
	prepareByteScratch(w * channels, h);
	for (l_t y = 0; y < h; ++y) {
		for (l_t x = 0; x < w; ++x) {
			uint8_t const * const source = &pixels[(y * w + x) * channels];
			uint8_t *dest = &scratch[(y * w + x) * channels];
			calculate(dest, source);
		}
	}
	return {(size_t)(scratchByteWidth * scratchHeight), scratch};
}

ROBytes TrueColorImage::data() const {
	return {(size_t)(w * h * channels), pixels};
}

ROBytes TrueColorImage::dataPremultiplied() const {
	return calculateData(premultiply);
}

ROBytes TrueColorImage::dataTint(uint8_t cr, uint8_t cg, uint8_t cb) const {
	c_t const colors[] {cb, cg, cr};
	return calculateData([&](uint8_t *dest, uint8_t const * const source) {
		tint(dest, source, colors);
	});
}

ROBytes TrueColorImage::dataPremultipliedTint(uint8_t cr, uint8_t cg, uint8_t cb) const {
	c_t const colors[] {cb, cg, cr};
	return calculateData([&](uint8_t *dest, uint8_t const * const source) {
		tint(dest, source, colors);
		premultiply(dest, dest);
	});
}

l_t TrueColorImage::getWidth() const {
	return w;
}

l_t TrueColorImage::getHeight() const {
	return h;
}

void TrueColorImage::clear() {
	memset(pixels, 0, w * h * channels);
}

void TrueColorImage::clear(l_t x, l_t y, l_t w0, l_t h0) {
	assert(x >= 0);
	assert(y >= 0);
	assert(x + w0 <= w);
	assert(y + h0 <= h);
	for (l_t y1 = y; y1 < y + h0; ++y1) {
		memset(
			&pixels[(y1 * w + x) * channels],
			0,
			w0 * channels
		);
	}
}

void TrueColorImage::serialize(const char *path) const throw(std::runtime_error) {
	png_image img;
	memset(&img, 0, sizeof(img));
	img.version = PNG_IMAGE_VERSION;
	img.width = w;
	img.height = h;
	img.format = PNG_FORMAT_BGRA;
	img.flags = 0;
	img.colormap_entries = 0;
	png_image_write_to_file(&img, path, false, pixels, w * channels, nullptr);
	if (PNG_IMAGE_FAILED(img)) {
		throw std::runtime_error(std::string(img.message));
	}
	if (img.warning_or_error != 0) {
		printf("libpng warning: %s\n", img.message);
	}
}

void TrueColorImage::setPixel(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, int x, int y) {
	auto *pixel = &pixels[(y * w + x) * channels];
	pixel[0] = cb;
	pixel[1] = cg;
	pixel[2] = cr;
	pixel[3] = ca;
}

uint8_t TrueColorImage::getPixelR(int x, int y) const {
	auto *pixel = &pixels[(y * w + x) * channels];
	return pixel[2];
}

uint8_t TrueColorImage::getPixelG(int x, int y) const {
	auto *pixel = &pixels[(y * w + x) * channels];
	return pixel[1];
}

uint8_t TrueColorImage::getPixelB(int x, int y) const {
	auto *pixel = &pixels[(y * w + x) * channels];
	return pixel[0];
}

uint8_t TrueColorImage::getPixelA(int x, int y) const {
	auto *pixel = &pixels[(y * w + x) * channels];
	return pixel[3];
}

void TrueColorImage::strokeSoft(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1_0, double y1_0, double r1_0, double x2_0, double y2_0, double r2_0, double blend) {
	uint8_t const colors[] {cb, cg, cr, ca};

	int const subpixels = 2;
	int const subpixelSum = msq(subpixels);
	int32_t offsetX;
	int32_t offsetY;
	int subpixelTop;
	int subpixelBottom;
	int subpixelLeft;
	int subpixelRight;
	strokeSolid(
		x1_0, y1_0, r1_0, x2_0, y2_0, r2_0, subpixels,
		[&](int const offsetX_0, int const offsetY_0, int const subpixelTop_0, int const subpixelBottom_0, int const subpixelLeft_0, int const subpixelRight_0) {
			offsetX = offsetX_0;
			offsetY = offsetY_0;
			subpixelTop = subpixelTop_0;
			subpixelBottom = subpixelBottom_0;
			subpixelLeft = subpixelLeft_0;
			subpixelRight = subpixelRight_0;
			//printf("got extents: o %d %d, t %d b %d l %d r %d\n", offsetX, offsetY, subpixelTop, subpixelBottom, subpixelLeft, subpixelRight);
			prepareBitScratch(subpixelRight, subpixelBottom);
		},
		[](c_t const y, float const start, float const end) {
			setBitScratchLine(y, std::floor(start), std::ceil(end));
		}
	);

	// merge + scale down (w/h / 2)
	for (
		l_t y = std::max(-offsetY, subpixelTop / subpixels);
		y < std::min(h - offsetY, posCeilDiv(subpixelBottom, subpixels));
		++y
	) {
		for (
			l_t x = std::max(-offsetX, subpixelLeft / subpixels);
			x < std::min(w - offsetX, posCeilDiv(subpixelRight, subpixels));
			++x
		) {
			//printf("compose at %d %d\n", offsetX + x, offsetY + y);
			uint8_t *pixel = &pixels[((offsetY + y) * w + (offsetX + x)) * channels];
			int subpixelCount = 0;
			for (int subY = 0; subY < subpixels; ++subY) {
				for (int subX = 0; subX < subpixels; ++subX) {
					subpixelCount += getBitScratch(
						x * subpixels + subX,
						y * subpixels + subY
					);
				}
			}
			float const useBlend = blend * (subpixelCount / (float)subpixelSum);
			float const compBlend = 1.0f - useBlend;
			float const sourceAlpha = (colors[3] / 255.0) * useBlend;
			float const destAlpha = (pixel[3] / 255.0) * compBlend;
			float const alphaSum = std::max(0.00001f, sourceAlpha + destAlpha);
			/*
			printf("composing dest %u %u %u %u; src %u %u %u %u; dest alpha %f, source alpha %f, blend %f\n",
				pixel[0],
				pixel[1],
				pixel[2],
				pixel[3],
				colors[0],
				colors[1],
				colors[2],
				colors[3],
				destAlpha,
				sourceAlpha,
				useBlend
			);
			*/
			for (int i = 0; i < channels - 1; ++i) {
				float const destValue = pixel[i] * destAlpha;
				float const sourceValue = colors[i] * sourceAlpha;
				float const sum = destValue + sourceValue;
				float const scaled = sum / alphaSum;
				uint8_t const byte = scaled;
				/*
				printf("\tdest fact %f, source fact %f, sum %f, result %f, byte %u\n",
					destValue, sourceValue, sum, scaled, byte);
				*/
				pixel[i] = byte;
			}
			pixel[3] = pixel[3] * compBlend + colors[3] * useBlend;
		}
	}
}

void TrueColorImage::strokeHard(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1_0, double y1_0, double r1_0, double x2_0, double y2_0, double r2_0, double blend) {
	uint8_t const colors[] {cb, cg, cr, ca};
	strokeSolidHard(
		x1_0, y1_0, r1_0, x2_0, y2_0, r2_0,
		[&](l_t y, l_t x) {
			assert(x >= 0);
			assert(x < w);
			for (int i = 0; i < channels; ++i) {
				pixels[(y * w + x) * channels + i] = colors[i];
			}
		}
	);
}

static inline void compose(uint8_t *dest, c_t const d_w, c_t const d_h, uint8_t const * const source, c_t const s_w, c_t const s_h, int32_t x0, int32_t y0, double opacity) {
	::merge(
		dest, channels, d_w, d_h, source, s_w, s_h, x0, y0,
		[opacity](uint8_t *dest, uint8_t const * const source, int span) {
			for (l_t x1 = 0; x1 < span; ++x1) {
				//printf("\tcomp %d %d, dest %d %d, source %d %d span span %d %d\n", x1, y1, x.dest + x1, y.dest + y1, x.source + x1, y.source + y1, x.span, y.span);
				uint8_t * const destPixel = &dest[x1 * channels];
				uint8_t const * const sourcePixel = &source[x1 * channels];
				float const sourceAlpha = sourcePixel[3] / 255.0 * opacity;
				float const destAlpha = destPixel[3] / 255.0;
				float const outAlpha = destAlpha * (1.0 - sourceAlpha) + sourceAlpha;
				/*
				printf("composing dest %u %u %u %u; src %u %u %u %u; dest alpha %f, source alpha %f, out alpha %f\n",
					destPixel[0],
					destPixel[1],
					destPixel[2],
					destPixel[3],
					sourcePixel[0],
					sourcePixel[1],
					sourcePixel[2],
					sourcePixel[3],
					destAlpha,
					sourceAlpha,
					outAlpha
				);
				*/
				for (int i = 0; i < channels - 1; ++i) {
					float const destValue = destPixel[i] * destAlpha * (1.0 - sourceAlpha);
					float const sourceValue = sourcePixel[i] * sourceAlpha;
					float const sum = destValue + sourceValue;
					float const scaled = sum / outAlpha;
					uint8_t const byte = scaled;
					/*
					printf("\tdest fact %f, source fact %f, sum %f, result %f, byte %u\n",
						destValue, sourceValue, sum, scaled, byte);
					*/
					destPixel[i] = byte;
				}
				destPixel[3] = std::min(255, (int)(255 * outAlpha));
			}
		}
	);
}

void TrueColorImage::replace(TrueColorImage const & source, int32_t x, int32_t y) {
	::replace(pixels, channels, w, h, source.pixels, source.w, source.h, x, y);
}

void TrueColorImage::compose(TrueColorImage const &source, int32_t x, int32_t y, double opacity) {
	::compose(pixels, w, h, source.pixels, source.w, source.h, x, y, opacity);
}

void TrueColorImage::compose(PaletteImage const & source, PaletteColors const & palette, int32_t x, int32_t y, double opacity) {
	auto sourcePixels = source.data(palette);
	::compose(pixels, w, h, sourcePixels.data, source.getWidth(), source.getHeight(), x, y, opacity);
}
