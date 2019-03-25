#include "header.hxx"
#include <memory>
#include <cassert>

int main(int argc, char **argv) {
	std::unique_ptr<TrueColorImage> i(TrueColorImage::create(500, 500));

	int const pad = 5;
	int maxX = 0;
	int x = pad;
	int y = pad;

	auto const draw = [&](float const x1, float const y1, float const r1, float const x2, float const y2, float const r2) {
		assert(r1 < pad);
		assert(r2 < pad);
		i->strokeSoft(
			255, 127, 0, 255,
			x + x1, y + y1, r1,
			x + x2, y + y2, r2,
			1
		);

		maxX = std::max(maxX, (int)(x + std::max(x1 + r1, x2 + r2)));
		y += std::max(y1 + r1, y2 + r1) + pad;
	};
	auto const colFeed = [&]() {
		x = maxX + pad;
		y = pad;
	};
	
	// 1 - 3px concentric
	draw(
		5, 5, 0.5,
		5, 5, 0.5
	);
	draw(
		5, 5, 1.5 / 2,
		5, 5, 1.5 / 2
	);
	draw(
		0, 0, 3,
		2, 0, 0.5
	);
	draw(
		0, 0, 3,
		2.5, 0, 0.5
	);

	// 1px vert, horiz
	draw(
	       	0, 0, 0.5,
	       	5, 0, 0.5
	);
	draw(
	       	5, 0, 0.5,
	       	0, 0, 0.5
	);
	draw(
	       	0, 0, 0.5,
	       	0, 5, 0.5
	);
	draw(
	       	0, 5, 0.5,
	       	0, 0, 0.5
	);
	
	// 1px - 3px vert, horiz
	draw(
	       	0, 0, 0.5,
	       	5, 0, 1.5
	);
	draw(
	       	0, 0, 1.5,
	       	5, 0, 0.5
	);
	draw(
	       	5, 0, 0.5,
	       	0, 0, 1.5
	);
	draw(
	       	5, 0, 1.5,
	       	0, 0, 0.5
	);
	draw(
	       	0, 0, 0.5,
	       	0, 5, 1.5
	);
	draw(
	       	0, 0, 1.5,
	       	0, 5, 0.5
	);
	draw(
	       	0, 5, 0.5,
	       	0, 0, 1.5
	);
	draw(
	       	0, 5, 1.5,
	       	0, 0, 0.5
	);

	// 1 - 3px top aligned, bottom aligned
	draw(
	       	0, 0, 0.5,
	       	5, 1, 1.5
	);
	draw(
	       	0, 1, 1.5,
	       	5, 0, 0.5
	);
	draw(
	       	5, 1, 1.5,
	       	0, 0, 0.5
	);
	draw(
	       	5, 0, 0.5,
	       	0, 1, 1.5
	);
	draw(
	       	0, 1, 0.5,
	       	5, 0, 1.5
	);
	draw(
	       	0, 0, 1.5,
	       	5, 1, 0.5
	);
	draw(
	       	5, 0, 1.5,
	       	0, 1, 0.5
	);
	draw(
	       	5, 1, 0.5,
	       	0, 0, 1.5
	);

	// Middle circ-circ overlap
	draw(
		0, 0, 1.5,
		3, 1.5, 1.5
	);
	draw(
		0, 1.5, 1.5,
		3, 0, 1.5
	);
	draw(
		3, 0, 1.5,
		0, 1.5, 1.5
	);
	draw(
		3, 1.5, 1.5,
		0, 0, 1.5
	);

	try {
		i->serialize("test.png");
	} catch (std::runtime_error const &e) {
		printf("Failed to serialize: %s", e.what());
	}

	std::unique_ptr<TrueColorImage> j(TrueColorImage::create(2, 3));
	j->strokeSoft(255, 255, 255, 255, -0.25, 1.5, 0.5, -0.25, 1.5, 0.5, 1);
	for (int y = 0; y < j->getHeight(); ++y) {
		for (int x = 0; x < j->getWidth(); ++x) {
			printf("%02x ", j->getPixelR(x, y));
			printf("%02x ", j->getPixelG(x, y));
			printf("%02x ", j->getPixelB(x, y));
			printf("%02x ", j->getPixelA(x, y));
			printf(" ");
		}
		printf("\n");
	}
	assert(j->getPixelR(1, 0) == 0);

	return 0;
}

