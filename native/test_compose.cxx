#include "header.hpp"
#include <memory>
#include <cassert>
#include <sstream>

int main(int argc, char **argv) {

	int testId = 0;

	auto const buildImage1 = [](int w, int h) -> std::unique_ptr<TrueColorImage> {
		std::unique_ptr<TrueColorImage> i{TrueColorImage::create(w, h)};
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				if ((x + y) % 2 == 1) continue;
				i->setPixel(255, 0, 0, 255, x, y);
			}
		}
		return i;
	};
	auto const buildImage2 = [](int w, int h) -> std::unique_ptr<TrueColorImage> {
		std::unique_ptr<TrueColorImage> i{TrueColorImage::create(w, h)};
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				if ((x + y) % 2 == 0) continue;
				i->setPixel(0, 0, 255, 255, x, y);
			}
		}
		return i;
	};

	auto const compose2 = [&](int w1, int h1, int w2, int h2, int x, int y) {
		auto i = buildImage1(w1, h1);
		auto j = buildImage2(w2, h2);

		i->compose(*j, x, y, 0.5);

		try {
			auto s = std::stringstream{};
			s << "compose_test" << testId++ << ".png";
			i->serialize(s.str().c_str());
		} catch (std::runtime_error const &e) {
			printf("Failed to serialize: %s", e.what());
		}
	};

	auto const compose = [&](int x, int y) {
		compose2(10, 10, 10, 10, x, y);
	};

	compose(0, 0);
	compose(5, 4);
	compose(-5, 0);
	compose(0, -5);
	compose(-5, -4);

	compose(-10, 0);
	compose(0, -10);
	compose(10, 0);
	compose(0, 10);
	compose(10, 10);
	compose(-10, 10);
	compose(10, -10);
	compose(-10, -10);

	compose2(20, 10, 10, 10, 0, 0); // Valgrind to check for reads after source

	compose2(512, 256, 256, 256, 0, 0); // Same as above, but causes segv

	compose2(20, 10, 10, 10, 10, 0);

	return 0;
}

