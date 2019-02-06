#include "header.hxx"
#include <cassert>
#include <memory>
#include <sstream>

int main(int argc, char **argv) {
	int testId = 0;

	auto const draw = [&](float const x, float const y, float const r) {
		std::unique_ptr<TrueColorImage> i(TrueColorImage::create(10, 10));
		i->strokeSoft(
			255, 127, 0, 255,
			x, y, r,
			x, y, r,
			1
		);
		try {
			auto s = std::stringstream{};
			s << "stroke_test_2_" << testId++ << ".png";
			i->serialize(s.str().c_str());
		} catch (std::runtime_error const &e) {
			printf("Failed to serialize: %s", e.what());
		}
	};

	draw(5, 0, 2);
	draw(0, 5, 2);
	draw(10, 5, 2);
	draw(5, 10, 2);

	return 0;
}

