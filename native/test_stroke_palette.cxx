#include "header.hxx"
#include <memory>
#include <cassert>

int main(int argc, char **argv) {
	std::unique_ptr<PaletteImage> i(PaletteImage::create(500, 500));

	int const pad = 5;
	int maxX = 0;
	int x = pad;
	int y = pad;

	auto const draw = [&](float const x1, float const y1, float const r1, float const x2, float const y2, float const r2) {
		assert(r1 < pad);
		assert(r2 < pad);
		i->stroke(
			1,
			x + x1, y + y1, r1,
			x + x2, y + y2, r2
		);

		maxX = std::max(maxX, (int)(x + std::max(x1 + r1, x2 + r2)));
		y += std::max(y1 + r1, y2 + r1) + pad;
	};

	draw(
		5, 5, 0.5,
		5, 5, 0.5
	);
	draw(
	       	0, 0, 0.5,
	       	5, 0, 0.5
	);

	try {
		i->serialize("test.bin");
	} catch (std::runtime_error const &e) {
		printf("Failed to serialize: %s", e.what());
	}

	std::unique_ptr<PaletteImage> j(PaletteImage::create(2, 3));
	j->stroke(1, -0.25, 1.5, 0.5, -0.25, 1.5, 0.5);
	for (int y = 0; y < j->getHeight(); ++y) {
		for (int x = 0; x < j->getWidth(); ++x) {
			p_t pix = j->getPixel(x, y);
			for (int c = 0; c < 4; ++c) {
				printf("%02x ", ((uint8_t *)&pix)[c]);
			}
			printf(" ");
		}
		printf("\n");
	}
	assert(j->getPixel(1, 0) == 0);
	return 0;
}

