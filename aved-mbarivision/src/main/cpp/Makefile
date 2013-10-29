##### Mbarivision Makefile
PREFIX      := /usr/local/aved
SALIENCYROOT := /home/avedac/dev/saliency-20131015
XERCESCROOT  := /home/avedac/dev/Xerces-C_2_7_0
SRCDIR      := src/
OBJDIR	    := target/build/obj/
BINDIR	    := target/build/bin/
LIBDIR      := target/build/lib/
SCHEMADIR   := schema/
CXX         := /opt/centos/devtoolset-1.1/root/usr/bin/g++
#CXXFLAGS    := -std=c++11 -march=nocona -I /opt/centos/devtoolset-1.1/root/usr/include/c++/4.7.2/
CXXFLAGS               :=  -Wall -g -I /opt/centos/devtoolset-1.1/root/usr/include/c++/4.7.2/ -O3 -std=c++11 -march=nocona
DEFS        := 
#CPPFLAGS    := -I /opt/centos/devtoolset-1.1/root/usr/include/c++/4.7.2/ -I$(SALIENCYROOT)/src -I$(XERCESCROOT)/src -I$(SRCDIR) -include $(SALIENCYROOT)/config.h
CPPFLAGS               := -I /opt/centos/devtoolset-1.1/root/usr/include/c++/4.7.2/  -Isrc/ -I$(XERCESCROOT)/src -I$(SALIENCYROOT)/src -include $(SALIENCYROOT)/config.h
LDFLAGS     := -L /opt/centos/devtoolset-1.1/root/usr/lib/gcc/x86_64-redhat-linux/4.7.2/ -L/usr/local/lib64 -L/usr/local/lib -L$(SALIENCYROOT)/build/obj -L$(XERCESCROOT)/lib -lxerces-c 
DEPFILE	    := alldepends
COMPILE1    := @echo 
COMPILE2    := @
COMPILE3    := 
CDEPS	    := $(BINDIR)cdeps

all: $(CDEPS) $(BINDIR)mbarivision

# for the compilation of the Version file every time to date/time stamp the build
$(OBJDIR)Utils/Version.o: force $(SRCDIR)Utils/Version.C
force: ;

##### Implicit rules
$(BINDIR)%:
	 $(COMPILE1) "Linking executable program" $@	 
	 $(COMPILE2) $(CXX)  \
	  -o $@ -O \
  	  $(addprefix $(PWD)/,$(filter-out /%, $+)) \
	  $(filter /%, $+)  \
	  $(LDFLAGS)  $(LIBS)
	  @mkdir -p $(BINDIR)/$(SCHEMADIR)
	  $(COMPILE1) "Installing xml schema files in $(SCHEMADIR) to $(BINDIR) "
	  @cp -Rf $(SCHEMADIR) $(BINDIR)
	  $(COMPILE1) "Done !"
	  
# make an object file from a C++ source file
$(OBJDIR)%.o: $(SRCDIR)%.C
	@mkdir -p $(dir $@)
	$(COMPILE1) "Compiling C++ file        " $<
	$(COMPILE1) $(CXX) $(DEFS) $(CPPFLAGS) $(CXXFLAGS) \
		-c $< \
		-o $@
	$(COMPILE2) $(CXX) $(DEFS) $(CPPFLAGS) $(CXXFLAGS) \
		-c $< \
		-o $@
	$(COMPILE3)

##### Rule to build the dependency-computing program:
$(CDEPS): cdeps.cc
	@mkdir -p $(BINDIR)
	$(COMPILE1) Compiling source dependencies calculator
	$(COMPILE2) $(CXX) -O2 -Wall $^ -o $@ 

#################################################################
##### Rule to build the source file dependencies ####
#### To add a new executable, add the line --execformat, 
#### e.g. to add a new test file build from the source test.C
#### --exeformat "$(SRCDIR)test.C : $(BINDIR)test
#################################################################
$(DEPFILE):$(CDEPS)
	$(COMPILE1) Computing source file dependencies
	$(COMPILE2) $(CDEPS) \
           --verbosity 0 \
           --srcdir "$(SRCDIR)" \
           --includedir "$(SRCDIR)" \
           --exeformat "$(SRCDIR)Mbarivision.C : $(BINDIR)mbarivision" \
           --includedir "$(SALIENCYROOT)/src" \
           --includedir "$(XERCESCROOT)/src" \
           --options-file depoptions-all \
           --objdir "$(OBJDIR)" \
           --objext ".o" \
           --literal .I \
           --linkformat "$(SALIENCYROOT)/src/ : $(SALIENCYROOT)/build/obj/*.o" \
           --linkformat "$(SRCDIR) : $(OBJDIR)*.o" \
           --output-link-deps \
           --output-compile-deps \
           --sources-variable SOURCES \
           --headers-variable HEADERS \
            --config-file $(SALIENCYROOT)/config.h \
            > $(@)

##### Explicit rules
# Grab the flags from the saliency build
LDFLAGS :=`grep -m 1 LDFLAGS $(SALIENCYROOT)/Makefile | cut -f2 -d =` $(LDFLAGS)

.PHONY: clean allclean uninstall

clean	:
	@( if [ -d $(BINDIR) ];then \
		rm -rf $(BINDIR)*; \
	fi )
	@( if [ -d $(OBJDIR) ];then \
		rm -rf $(OBJDIR)*; \
	fi )

cleanobjs : 
	@( if [ -d $(OBJDIR) ];then \
		rm -rf $(OBJDIR)*; \
	fi )


depclean:
	@-rm -f $(DEPFILE) 

allclean: clean depclean

install: 
	$(COMPILE1) "Installing mbarivision in $(BINDIR) to $(PREFIX)/bin "
	@mkdir -p $(PREFIX)/bin/$(SCHEMADIR)
	@cp -f $(BINDIR)mbarivision $(PREFIX)/bin
	$(COMPILE1) "Installing mbarivision xml schema files in $(SCHEMADIR) to $(PREFIX)/bin "
	@cp -Rf $(SCHEMADIR) $(PREFIX)/bin
	$(COMPILE1) "Done !"

uninstall: 
	$(COMPILE1) "Removing mbarivision in $(PREFIX)/bin "
	@rm -rf $(PREFIX)/bin/$(SCHEMADIR)
	@rm -f $(PREFIX)/bin/mbarivision
	$(COMPILE1) "Done !"
	
-include $(DEPFILE)

