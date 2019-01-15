#include "header.hpp"

#include <algorithm>
#include <cstring>
#include <string>

#include <png.h>

static int const channels = 4;

ROBytes::ROBytes(size_t const size, uint8_t const * const data) : size(size), data(data) {
}

TrueColorImage::TrueColorImage(int w, int h, uint8_t * const pixels) : w(w), h(h), pixels(pixels) {
}

TrueColorImage::TrueColorImage(TrueColorImage &&other) noexcept : w(other.w), h(other.h), pixels(other.pixels) {
	other.pixels = nullptr;
}

TrueColorImage::~TrueColorImage() {
	delete [] pixels;
}

TrueColorImage * TrueColorImage::create(int w, int h) {
	return new TrueColorImage(w, h, new uint8_t[w * h * channels]);
}

TrueColorImage * TrueColorImage::deserialize(char const * const path) throw(std::runtime_error) {
	png_image img;
	memset(&img, 0, sizeof(img));
	img.version = PNG_IMAGE_VERSION;
	img.format = PNG_FORMAT_RGBA;
	if (!png_image_begin_read_from_file(&img, path)) {
		throw std::runtime_error(std::string(img.message));
	}
	auto pixels = new uint8_t[PNG_IMAGE_SIZE(img)];
	if (!png_image_finish_read(&img, nullptr, pixels, 0, nullptr)) {
		throw std::runtime_error(std::string(img.message));
	}
	return new TrueColorImage(img.width, img.height, pixels);
}

TrueColorImage * TrueColorImage::copy(int x0, int y0, int w0, int h0) const {
	TrueColorImage * out = TrueColorImage::create(w0, h0);
	for (int y1 = 0; y1 < h0; ++y1) {
		memcpy(
			&out->pixels[y1 * out->w * channels],
			&pixels[(y0 + y1) * w * channels + x0],
			out->w * channels
		);
	}
	return out;
}

ROBytes TrueColorImage::data() const {
	return ROBytes(w * h * channels, pixels);
}

int TrueColorImage::getWidth() const {
	return w;
}

int TrueColorImage::getHeight() const {
	return h;
}

void TrueColorImage::clear() {
	memset(pixels, 0, w * h * channels);
}

void TrueColorImage::serialize(const char *path) const throw(std::runtime_error) {
	png_image img;
	memset(&img, 0, sizeof(img));
	img.version = PNG_IMAGE_VERSION;
	img.width = w;
	img.height = h;
	img.format = PNG_FORMAT_RGBA;
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

void TrueColorImage::stroke(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1, double y1, double r1, double x2, double y2, double r2, double blend) {
}

void TrueColorImage::compose(TrueColorImage const &source, int x0, int y0, double opacity) {
	struct V {
		int source;
		int dest;
		int span;
	};
	auto calcV = [&](auto at, auto sourceSpan, auto destSpan) {
		V out{};
		if (at < 0) {
			out.source = -at;
			out.dest = 0;
			out.span = std::min(destSpan, sourceSpan + at);
		} else {
			out.source = 0;
			out.dest = at;
			out.span = std::min(destSpan, sourceSpan) - at;
		}
		return out;
	};
	V const x = calcV(x0, source.w, w);
	V const y = calcV(y0, source.h, h);
	for (int y1 = 0; y1 < y.span; ++y1) {
		for (int x1 = 0; x1 < x.span; ++x1) {
			uint8_t * destPixel = &pixels[((y.dest + y1) * w + (x.dest + x1)) * channels];
			uint8_t * sourcePixel = &source.pixels[((y.source + y1) * w + (x.source + x1)) * channels];
			double useOpacity = (sourcePixel[3] / 255.0f) * opacity;
			for (int i = 0; i < channels - 1; ++i) {
				destPixel[i] = destPixel[i] * (1.0 - useOpacity) + sourcePixel[i] * useOpacity;
			}
			destPixel[3] = std::min(255, (int)(destPixel[3] * (1.0 - useOpacity) + sourcePixel[3]));
		}
	}
}
