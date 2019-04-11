#include <memory>
#include <cassert>
#include <iostream>

#include "gzutil.hxx"


template <class A, class B> static void assert2(A const &a, B const &b) {
	if (a != b) {
		std::cerr << "got " << a << std::endl << "expected " << b << std::endl << std::flush;
		assert(false);
	}
}

int main(int argc, char **argv) {
	{
		GZ gz{"test.gz", "w"};
		gz.write<int32_t>((int32_t) 4);
	}
	{
		GZ gz{"test.gz", "r"};
		auto got = gz.read<int32_t>();
		assert2(got, 4);
	}
	return 0;
}

