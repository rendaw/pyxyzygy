#include "header.hxx"
#include <memory>
#include <cassert>
#include <sstream>

int main(int argc, char **argv) {
	std::unique_ptr<TrueColorImage> i{TrueColorImage::create(1, 1)};
	i->setPixel(255, 127, 0, 255, 0, 0);
	{
		ROBytes bytes(i->dataPremultipliedTint(0u, 0u, 255u));
		printf("%u %u %u %u\n", bytes.data[0], bytes.data[1], bytes.data[2], bytes.data[3]);
	}
	{
		ROBytes bytes(i->dataTint(0u, 0u, 255u));
		printf("%u %u %u %u\n", bytes.data[0], bytes.data[1], bytes.data[2], bytes.data[3]);
	}
	return 0;
}

