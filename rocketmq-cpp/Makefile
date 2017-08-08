LIBS_ORIG := $(patsubst %/,%, $(dir $(wildcard rpm/Makefile) $(wildcard libs/*/Makefile)))
LIBS_CLEAN := $(addsuffix -clean,$(LIBS_ORIG))

.PHONY: $(LIBS_ORIG) $(LIBS_CLEAN)

all: build-libs build-shared build-example

build-libs: $(LIBS_ORIG)

$(LIBS_ORIG):
	$(MAKE) -C $@

build-shared:
	$(MAKE) -C project

build-example:
	$(MAKE) -C example

test:
	@echo $(LIBS_CLEAN)

# clean:$(LIBS_CLEAN)
clean:
	$(MAKE) -C project clean
	$(MAKE) -C example clean
	$(MAKE) -C bin clean
	$(RM) -rf  logs/*.log
	$(RM) -rf  tmp

cleanall:$(LIBS_CLEAN) clean

install:
	$(MAKE) -C project install

$(LIBS_CLEAN):
	$(MAKE) -C $(@:-clean=) clean
