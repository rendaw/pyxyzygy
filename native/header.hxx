#ifdef SWIG
%module mynative
%{
#include "header.hxx"
%}

%include <std_except.i>

%apply jbyte { uint8_t }
%apply int { l_t }
%apply int { c_t }
%apply int { int32_t }
%apply int { p_t }

%ignore ROBytes;

%typemap(jni) ROBytes "jbyteArray"
%typemap(jtype) ROBytes "byte[]"
%typemap(jstype) ROBytes "byte[]"
%typemap(javaout) ROBytes {
    return $jnicall;
  }

%typemap(out) ROBytes %{
  $result = jenv->NewByteArray($1.size);
  if (!$result) return $null;
  jenv->SetByteArrayRegion($result, 0, $1.size, (jbyte *)$1.data);
%}

%typemap(jni) (uint8_t *DATA, size_t LENGTH) "jbyteArray"
%typemap(jtype) (uint8_t *DATA, size_t LENGTH) "byte[]"
%typemap(jstype) (uint8_t *DATA, size_t LENGTH) "byte[]"
%typemap(javain) (uint8_t *DATA, size_t LENGTH) "$javainput"
%typemap(freearg) (uint8_t *DATA, size_t LENGTH) ""
%typemap(in) (uint8_t *DATA, size_t LENGTH) {
  if ($input) {
    $1 = ($1_ltype) JCALL2(GetByteArrayElements, jenv, $input, 0);
    $2 = ($2_type) JCALL1(GetArrayLength, jenv, $input);
  } else {
    $1 = 0;
    $2 = 0;
  }
}
%typemap(argout) (uint8_t *DATA, size_t LENGTH) {
  if ($input) JCALL3(ReleaseByteArrayElements, jenv, $input, (jbyte *)$1, 0);
}
%typemap(directorin, descriptor="[B", noblock=1) (uint8_t *DATA, size_t LENGTH) {
  $input = 0;
  if ($1) {
    $input = JCALL1(NewByteArray, jenv, (jsize)$2);
    if (!$input) return $null;
    JCALL4(SetByteArrayRegion, jenv, $input, 0, (jsize)$2, (jbyte *)$1);
  }
  Swig::LocalRefGuard $1_refguard(jenv, $input);
}
%typemap(directorargout, noblock=1) (uint8_t *DATA, size_t LENGTH)
{ if ($input && $1) JCALL4(GetByteArrayRegion, jenv, $input, 0, (jsize)$2, (jbyte *)$1); }
%typemap(javadirectorin, descriptor="[B") (uint8_t *DATA, size_t LENGTH) "$jniinput"

#endif

#include <vector>
#include <cstdint>
#include <stdexcept>

struct ROBytes {
	size_t const size;
	uint8_t const * data;

	ROBytes(size_t const size, uint8_t const * data);
};

typedef uint32_t p_t;
typedef uint32_t c_t;
typedef int32_t l_t;

class PaletteColors {
	public:
		void set(p_t index, uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca);
		c_t get(p_t index) const;
	private:
		std::vector<std::pair<p_t, c_t>> colors;
};

class PaletteImage {
	public:
		~PaletteImage();
		static PaletteImage * create(l_t w, l_t h);
		static PaletteImage * deserialize(char const * path) throw(std::runtime_error);
		PaletteImage * copy(l_t x, l_t y, l_t w, l_t h) const;
		ROBytes data(PaletteColors const &palette) const;
		ROBytes dataPremultiplied(PaletteColors const &palette) const;
		ROBytes dataTint(PaletteColors const &palette, uint8_t cr, uint8_t cg, uint8_t cb) const;
		ROBytes dataPremultipliedTint(PaletteColors const &palette, uint8_t cr, uint8_t cg, uint8_t cb) const;
		l_t getWidth() const;
		l_t getHeight() const;
		void clear();
		void clear(l_t x, l_t y, l_t width, l_t height);
		void serialize(char const * path) const throw(std::runtime_error);
		void setPixel(p_t index, l_t x, l_t y);
		p_t getPixel(l_t x, l_t y) const;
		void stroke(p_t index, double x1, double y1, double r1, double x2, double y2, double r2);
		void mergeColor(p_t oldIndex, p_t newIndex);
		void replace(PaletteImage const & source, int32_t x, int32_t y);

		PaletteImage &operator = (PaletteImage const & other) = delete;
		PaletteImage &operator = (PaletteImage && other) = delete;
		PaletteImage(PaletteImage &&other) = delete;
		PaletteImage(PaletteImage const & other) = delete;
	private:
		PaletteImage(l_t w, l_t h, p_t * pixels);
		template <class T> ROBytes calculateData(T calculate) const;

		l_t const w;
		l_t const h;
		p_t * const pixels;
};

class TrueColorImage {
	public:
		~TrueColorImage();
		static TrueColorImage * create(l_t w, l_t h);
		static TrueColorImage * deserialize(char const * path) throw(std::runtime_error);
		TrueColorImage * copy(l_t x, l_t y, l_t w, l_t h) const;
		TrueColorImage * scale(int scale) const;
		ROBytes data() const;
		ROBytes dataPremultiplied() const;
		ROBytes dataTint(uint8_t cr, uint8_t cg, uint8_t cb) const;
		ROBytes dataPremultipliedTint(uint8_t cr, uint8_t cg, uint8_t cb) const;
		l_t getWidth() const;
		l_t getHeight() const;
		void clear();
		void clear(l_t x, l_t y, l_t width, l_t height);
		void serialize(char const * path) const throw(std::runtime_error);
		void setPixel(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, int x, int y);
		uint8_t getPixelR(int x, int y) const;
		uint8_t getPixelG(int x, int y) const;
		uint8_t getPixelB(int x, int y) const;
		uint8_t getPixelA(int x, int y) const;
		void strokeSoft(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1, double y1, double r1, double x2, double y2, double r2, double blend);
		void strokeHard(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1, double y1, double r1, double x2, double y2, double r2, double blend);
		void replace(TrueColorImage const & source, int32_t x, int32_t y);
		void compose(TrueColorImage const & source, int32_t x, int32_t y, double opacity);
		void compose(PaletteImage const & source, PaletteColors const & palette, int32_t x, int32_t y, double opacity);

		TrueColorImage &operator = (TrueColorImage const & other) = delete;
		TrueColorImage &operator = (TrueColorImage && other) = delete;
		TrueColorImage(TrueColorImage &&other) = delete;
		TrueColorImage(TrueColorImage const & other) = delete;
	private:
		TrueColorImage(l_t w, l_t h, uint8_t * pixels);
		template <class T> ROBytes calculateData(T calculate) const;

		l_t const w;
		l_t const h;
		uint8_t * const pixels;
};

#ifdef SWIG
%pragma(java) jniclasscode=%{
	static {
		System.load(com.zarbosoft.rendaw.common.Common.extractResource(mynative.class, "##OUTPUT##").toAbsolutePath().toString());
	}
%}
#endif