 dnl Process this file with autoconf to produce a configure script.
dnl This macro is a must

AC_INIT([pmbarivision.C], [dcline@mbari.org])
AC_PREREQ(2.50)
AC_PREFIX_DEFAULT(/home/aved)
AC_LANG(C++)

##########################################################################
### Some macro definitions

dnl AC_LATE_WARN(msg) msg will be given as a warning, and will be
dnl repeated at the end of running the configure script, making it more
dnl likely that the user will notice it.

dnl AC_CHECK_LIB_CXX(libname, includes, function-call,
dnl                  action-on-success, action-on-fail,
dnl                  extra-libs-for-link)
AC_DEFUN([AC_CHECK_LIB_CXX],
	 [AC_MSG_CHECKING([for lib$1])
	 libs_save=$LIBS
	 LIBS="$LIBS -l$1 $6"
	 AC_TRY_LINK([$2], [$3], [havelib_$1=yes], [havelib_$1=no])
	 LIBS=$libs_save
	 if test "x$havelib_$1" = xyes; then
	    AC_MSG_RESULT(yes)
	    $4
	 else
	    AC_MSG_RESULT(no)
	    $5
	 fi])

dnl AC_CHECK_HEADERS_CXX(aname, includes, function-call,
dnl                  action-on-success, action-on-fail)
AC_DEFUN([AC_CHECK_HEADERS_CXX],
	 [AC_MSG_CHECKING([for headers in $1])
	 AC_TRY_LINK([$2], [$3], [haveheaders_$1=yes], [haveheaders_$1=no])
	 LIBS=$libs_save
	 if test "x$haveheaders_$1" = xyes; then
	    AC_MSG_RESULT(yes)
	    $4
	 else
	    AC_MSG_RESULT(no)
	    $5
	 fi])

dnl This handles libraries with the dash in them. It's an alternative
dnl to the AC_CHECK_LIB_CXX above
dnl AC_CHECK_LIB_CXX_DASH(libname, includes, function-call,
dnl                  action-on-success, action-on-fail,
dnl                  extra-libs-for-link)
AC_DEFUN([AC_CHECK_LIB_CXX_DASH],
	 [AC_MSG_CHECKING([for lib$2])
	 libs_save=$LIBS
	 LIBS="$LIBS -l$2 $7"
	 AC_TRY_LINK([$3], [$4], [havelib_$1=yes], [havelib_$1=no])
	 LIBS=$libs_save
	 if test "x$havelib_$1" = xyes; then
	    AC_MSG_RESULT(yes)
	    $5
	 else
	    AC_MSG_RESULT(no)
	    $6
	 fi])

dnl Checks for c compiler.
AC_PROG_CC

dnl Checks for c compiler.
AC_PROG_CXX 

# Enable users to specify where Saliency is installed.
# TODO: Add saliency version check
# The default is in a sub directory "aved" in the users
# home directory
AC_ARG_WITH([saliency],
            [AC_HELP_STRING([--with-saliency=DIR],
                            [where the saliency is installed (e.g., $HOME/aved/saliency )
            [default=$HOME/aved/saliency]])],
            , 
	    [with_saliency=$HOME/aved/saliency])

# Use the supplied (or default) saliency root directory to set
# Makefile variables 
if test -d "$with_saliency" ; then
      
  saliency_incdir="-I$with_saliency/src"
  saliency_libdir="-L$with_saliency/build/obj"

  cppflags_save="$CPPFLAGS"
  ldflags_save="$LDFLAGS"
  CPPFLAGS="$CPPFLAGS $saliency_incdir"
  LDFLAGS="$LDFLAGS $saliency_libdir" 

  AC_CHECK_HEADERS_CXX(saliency, [
#include <Component/ModelManager.H>
#include <Neuro/StdBrain.H>
], [],
	               [with_saliency=$with_saliency],
                       [AC_MSG_ERROR([iLab Neuromorphic Vision C++ Toolkit
                        root directory $with_saliency" missing or incorrect.
                        Set the correct root path with --with-saliencyroot=DIR
                        For more information about getting the toolkit see http://ilab.usc.edu/toolkit/.])
                       ])
   else
      AC_MSG_ERROR([iLab Neuromorphic Vision C++ Toolkit
                        root directory $with_saliency" missing or incorrect.
                        Set the correct root path with --with-saliencyroot=DIR
                        For more information about getting the toolkit see http://ilab.usc.edu/toolkit/.])
   fi

CPPFLAGS="$cppflags_save"
LDFLAGS="$ldflags_save"

AC_SUBST(with_saliency)

# Enable users to specify where Xercesc is installed.
# Note the 2.7.0 version is required
# The default is to have the librariesin a sub directory "aved"
# in the users home directory
AC_ARG_WITH([xercesc],
            [AC_HELP_STRING([--with-xercesc=DIR],
                            [where the Xercesc is installed (e.g., $HOME/aved/Xerces-C_2_7_0 )
            [default=$HOME/aved/Xerces-C_2_7_0]])],
            ,
            [with_xercesc=$HOME/aved/Xerces-C_2_7_0])
 
# Use the supplied (or default) Xercesc root directory to set
# makefile variables
if test -d "$with_xercesc" ; then

   xercesc_incdir="$with_xercesc/src/xercesc"
   xercesc_libdir="$with_xercesc/lib"

   cppflags_save="$CPPFLAGS"
   ldflags_save="$LDFLAGS"
   CPPFLAGS="$CPPFLAGS -I$xercesc_incdir"
   LDFLAGS="$LDFLAGS -L$xercesc_libdir"

   AC_CHECK_LIB_CXX_DASH(xercesc, xerces-c, [
#include <xercesc/util/OutOfMemoryException.hpp>
#include <xercesc/framework/LocalFileFormatTarget.hpp>
#include <xercesc/util/XMLDateTime.hpp>
], [ ],
                       [with_xercesc=$with_xercesc],
                       [AC_MSG_ERROR([Xercesc directory "$with_xercesc" missing or incorrect.
                        Set the correct root path with --with-xercesc=DIR])
                       ],
                       [-lxerces-c -lpthread])
else		       
      AC_MSG_ERROR([Xercesc directory $with_xercesc missing or incorrect.
                    Set the correct root path with --with-xercesc=DIR   ])
fi

CPPFLAGS="$cppflags_save"
LDFLAGS="$ldflags_save"

AC_SUBST(with_xercesc)

# Enable users to specify where mbarivision is installed.
# The default is to be in the subdirectory aved in the Maven
# submodule aved-mbarivision
AC_ARG_WITH([xercesc],
            [AC_HELP_STRING([--with-mbarivision=DIR],
                            [where the mbarivision is installed (e.g., $HOME/aved/aved-mbarivision )
            [default=$HOME/aved/aved-mbarivision])],
            ,
            [with_mbarivision=$HOME/aved/aved-mbarivision])


# Use the supplied (or default) mbarivision root directory to appropriately
# set various build flags so that the compiler and linker can find the
# necessary headers and libraries.
if test -d "$mbarivision_root" ; then

    mbarivision_incdir="-I$mbarivision_root/src/main/cpp"
    mbarivision_libdir="-L$mbarivision_root/target/build/obj"

    cppflags_save="$CPPFLAGS"
    ldflags_save="$LDFLAGS"
    CPPFLAGS="$CPPFLAGS $mbarivision_incdir"
    LDFLAGS="$LDFLAGS $mbarivision_libdir"

   AC_CHECK_HEADERS_CXX(mbarivision, [ 
#include "DetectionAndTracking/DetectionParameters.H"
DetectionParameters detectionParms;
], [],
                       [with_mbarivision=$with_mbarivision],
                       [AC_MSG_ERROR([Mbarivision root directory "$with_mbarivision" missing or invalid.
		       	 Set the correct path with --with-mbarivision=DIR.])
                        CPPFLAGS="$cppflags_save"
                        LDFLAGS="$ldflags_save"],
                       )
else
    AC_MSG_ERROR([Mbarivision root directory "$with_mbarivision" missing or invalid. 
    	          Set the correct root path with --with-mbarivision=DIR.])
fi

CPPFLAGS="$cppflags_save"
LDFLAGS="$ldflags_save"

AC_SUBST(with_mbarivision)

# MPI compiler check
AC_PATH_PROGS([MPICXX], [mpicxx], [])

if test "x$MPICC" = "x" ; then
    AC_MSG_CHECKING([if mpicxx program was found])
    AC_MSG_RESULT(yes)
	AC_SUBST(MPICXX)
else   
    AC_MSG_ERROR([mpicxx program missing; Cannot build program.])
fi

dnl Create these files, making substitutions if necessary
AC_OUTPUT([Makefile])


