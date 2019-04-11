#include <string>
#include <cerrno>
#include <memory>
#ifdef WIN32
#include <winsock2.h>
#else
#include <netinet/in.h>
#endif
#include <cstring>

#include <zlib.h>

struct GZ {
	gzFile file;

	GZ(char const * const path, char const * const mode) : file(gzopen(path, mode)) {
		if (!file) throw std::runtime_error(std::string(strerror(errno)));
	}

	~GZ() {
		gzclose(file);
	}

	template <class prim> prim read() {
		static_assert(sizeof(prim) == 1 || sizeof(prim) == 2 || sizeof(prim) == 4, "");
		prim out;
		read((uint8_t *)&out, sizeof(out));
		if (sizeof(prim) == 1) return out;
		if (sizeof(prim) == 2) return ntohs(out);
		if (sizeof(prim) == 4) return ntohl(out);
	}

	template <class prim> prim *reada(size_t const count) {
		static_assert(sizeof(prim) == 1 || sizeof(prim) == 2 || sizeof(prim) == 4, "");
		prim *out = new prim[count];
		read((uint8_t *)out, sizeof(prim) * count);
		if (sizeof(prim) == 1) {}
		else if (sizeof(prim) == 2) {
			for (size_t i = 0; i < count; ++i) {
				out[i] = ntohs(out[i]);
			}
		} else if (sizeof(prim) == 4) {
			for (size_t i = 0; i < count; ++i) {
				out[i] = ntohl(out[i]);
			}
		}
		return out;
	}

	template <class prim> void write(prim const in) {
		static_assert(sizeof(prim) == 1 || sizeof(prim) == 2 || sizeof(prim) == 4, "");
		if (sizeof(prim) == 1)
			write((uint8_t *)&in, sizeof(prim));
		else {
			prim temp;
			if (sizeof(prim) == 2) temp = htons(in);
			else if (sizeof(prim) == 4) temp = htonl(in);
			write((uint8_t *)&temp, sizeof(prim));
		}
	}

	template <class prim> void writea(prim const * const in, size_t const count) {
		static_assert(sizeof(prim) == 1 || sizeof(prim) == 2 || sizeof(prim) == 4, "");
		if (sizeof(prim) == 1)
			write((uint8_t *)in, count);
		else {
			std::unique_ptr<prim> temp {new prim[count]};
			if (sizeof(prim) == 2) {
				for (size_t i = 0; i < count; ++i) {
					temp.get()[i] = htons(in[i]);
				}
			} else if (sizeof(prim) == 4) {
				for (size_t i = 0; i < count; ++i) {
					temp.get()[i] = htonl(in[i]);
				}
			}
			write((uint8_t *)temp.get(), count * sizeof(prim));
		}
	}

	private:
		void read(uint8_t *dest, size_t bytes) {
			while (bytes > 0) {
				int result = gzread(file, dest, bytes);
				if (result < 1) {
					throw std::runtime_error(std::string(gzerror(file, &result)));
				}
				dest += result;
				bytes -= result;
			}
		}

		void write(uint8_t const *source, size_t bytes) {
			while (bytes > 0) {
				int result = gzwrite(file, source, bytes);
				if (result <= 0) {
					throw std::runtime_error(std::string(gzerror(file, &result)));
				}
				source += result;
				bytes -= result;
			}
		}
};
