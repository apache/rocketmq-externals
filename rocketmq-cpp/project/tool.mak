define BUILD_LIBRARY
$(if $(wildcard $@),@$(RM) $@)
$(if $(wildcard ar.mac),@$(RM) ar.mac)
$(if $(filter %.a, $^),
@echo CREATE $@ > ar.mac
@echo SAVE >> ar.mac
@echo END >> ar.mac
@$(AR) -M < ar.mac
)
$(if $(filter %.o,$^),@$(AR) -q $@ $(filter %.o, $^))
$(if $(filter %.a, $^),
@echo OPEN $@ > ar.mac
$(foreach LIB, $(filter %.a, $^),
@echo ADDLIB $(LIB) >> ar.mac
)
@echo SAVE >> ar.mac
@echo END >> ar.mac
@$(AR) -M < ar.mac
@$(RM) ar.mac
)
endef