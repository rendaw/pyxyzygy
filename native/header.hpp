#ifdef SWIG
%module mynative
%{
#include "header.hpp"
%}

%apply jbyte { uint8_t }

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

#include <cstdint>
#include <stdexcept>

struct ROBytes {
	size_t const size;
	uint8_t const * data;

	ROBytes(size_t const size, uint8_t const * data);
};

class TrueColorImage {
	public:
		~TrueColorImage();
		static TrueColorImage * create(int w, int h);
		static TrueColorImage * deserialize(char const * path) throw(std::runtime_error);
		TrueColorImage * copy(int x, int y, int w, int h) const;
		ROBytes data(int const zoom);
		ROBytes dataPremultiplied(int const zoom);
		int getWidth() const;
		int getHeight() const;
		void clear();
		void serialize(char const * path) const throw(std::runtime_error);
		void setPixel(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, int x, int y);
		void stroke(uint8_t cr, uint8_t cg, uint8_t cb, uint8_t ca, double x1, double y1, double r1, double x2, double y2, double r2, double blend);
		void compose(TrueColorImage const & source, int x, int y, double opacity);

		TrueColorImage &operator = (TrueColorImage const & other) = delete;
		TrueColorImage &operator = (TrueColorImage && other) = delete;
		TrueColorImage(TrueColorImage &&other) = delete;
		TrueColorImage(TrueColorImage const & other) = delete;
	private:
		TrueColorImage(int w, int h, uint8_t * pixels);
		template <class T> ROBytes calculateZoomedData(int const zoom, T calculate);

		int const w;
		int const h;
		uint8_t * const pixels;
};

#ifdef SWIG
%pragma(java) jniclasscode=%{
  static {
    System.load(com.zarbosoft.rendaw.common.Common.extractResource(mynative.class, "mynative.so").toAbsolutePath().toString());
	System.out.println("Loaded mynative");
	System.out.flush();
  }
%}
#endif