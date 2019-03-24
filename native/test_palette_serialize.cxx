#include "header.hxx"
#include <memory>
#include <iostream>
#include <cassert>

template <class A, class B> static void assert2(A const &a, B const &b) {
	if (a != b) {
		std::cerr << "got " << a << std::endl << "expected " << b << std::endl << std::flush;
		assert(false);
	}
}

int main(int argc, char **argv) {
	std::unique_ptr<PaletteImage> i(PaletteImage::create(10, 10));
	i->setPixel(4, 3, 2);
	i->serialize("test.bin");
	std::unique_ptr<PaletteImage> j(PaletteImage::deserialize("test.bin"));
	assert2(j->getWidth(), i->getWidth());
	assert2(j->getHeight(), i->getHeight());
	assert2(j->getPixel(0, 0), 0);
	assert2(j->getPixel(1, 0), 0);
	assert2(j->getPixel(3, 2), 4);
	return 0;
}

