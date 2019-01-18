#include "header.hpp"
#include <memory>
#include <cassert>
#include <sstream>

int main(int argc, char **argv) {
	int const yMax = 10;
	std::unique_ptr<TrueColorImage> i{TrueColorImage::create(15, yMax)};
	for (int y = 0; y < yMax; ++y) {
		i->setPixel(255, 0, 0, 255, 8, y);
		i->setPixel(255, 0, 0, 127, 9, y);
		i->setPixel(255, 0, 0, 0, 10, y);
		i->setPixel(255, 0, 0, 10, 11, y);
	}
	i->stroke(
		0, 0, 255, 128,
		9, 5, 3,
		9, 5, 3,
		0.5
	);
	i->serialize("test_compose_stroke.png");
	return 0;
}

