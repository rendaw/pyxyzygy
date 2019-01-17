#include "header.hpp"
#include <memory>
#include <cassert>
#include <sstream>

int main(int argc, char **argv) {
	auto base = TrueColorImage::deserialize("../render-0-0-after.png");
	auto tile = TrueColorImage::deserialize("../render-1-0.png");
	base->compose(*tile, 256, 0, 1);
	base->serialize("test_compose2.png");
	return 0;
}

