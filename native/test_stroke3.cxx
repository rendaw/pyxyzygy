#include "header.hxx"
#include <cassert>
#include <memory>
#include <sstream>

int main(int argc, char **argv) {
	std::unique_ptr<TrueColorImage> i(TrueColorImage::create(10, 10));
	i->strokeSoft(
		255, 127, 0, 255,
		4, 4, 2,
		4, 4, 2,
		1
	);
	printf("\n\n stroke 2\n");
	i->strokeSoft(
		255, 127, 0, 255,
		6, 6, 2,
		6, 6, 2,
		1
	);
	i->serialize("test_stroke3.png");
	return 0;
}

