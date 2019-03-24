#include "header.hxx"
#include <memory>
#include <iostream>
#include <cassert>
#include <netinet/in.h>

template <class A, class B> static void assert2(A const &a, B const &b) {
	if (a != b) {
		std::cerr << "got " << a << std::endl << "expected " << b << std::endl << std::flush;
		assert(false);
	}
}

static void assert2b(uint32_t const &a, uint32_t const &b) {
	if (a != b) {
		printf("got %08x\nexpected %08x\n", a, b);
		fflush(stdout);
		assert(false);
	}
}

static void assert2b(uint8_t const &a, uint8_t const &b) {
	if (a != b) {
		printf("got %02x\nexpected %02x\n", a, b);
		fflush(stdout);
		assert(false);
	}
}

int main(int argc, char **argv) {
	PaletteColors c{};
	c.set(1, 0xFF, 0, 0, 0xFF);
	c.set(2, 0, 0xFF, 0, 0xFF);
	c.set(3, 0, 0, 0xFF, 0xFF);
	assert2b(c.get(1), htonl(0x0000FFFF));
	assert2b(c.get(0), htonl(0x00000000));
	std::unique_ptr<PaletteImage> i(PaletteImage::create(5, 5));
	i->setPixel(1, 0, 0);
	i->setPixel(2, 1, 0);
	i->setPixel(3, 2, 0);
	i->setPixel(1, 0, 2);
	i->setPixel(2, 1, 2);
	i->setPixel(3, 2, 2);

	auto d = i->data(c);

	for (int y = 0; y < 5; ++y) {
		for (int x = 0; x < 5; ++x) {
			printf("%08x ", *((uint32_t *)&d.data[(y * 5 + x) * 4]));
		}
		printf("\n");
	}
	for (int y = 0; y < 5; ++y) {
		for (int x = 0; x < 5; ++x) {
			for (int c = 0; c < 4; ++c) {
				printf("%02x ", d.data[(y * 5 + x) * 4 + c]);
			}
			printf(" ");
		}
		printf("\n");
	}

	assert2b(d.data[0], 0x00);
	assert2b(d.data[1], 0x00);
	assert2b(d.data[2], 0xFF);
	assert2b(d.data[3], 0xFF);

	assert2b(d.data[4], 0x00);
	assert2b(d.data[5], 0xFF);
	assert2b(d.data[6], 0x00);
	assert2b(d.data[7], 0xFF);

	assert2b(d.data[8], 0xFF);
	assert2b(d.data[9], 0x00);
	assert2b(d.data[10], 0x00);
	assert2b(d.data[11], 0xFF);

	return 0;
}

