package constant

const (
	PERM_PRIORITY = 0x1 << 3;
	PERM_READ = 0x1 << 2;
	PERM_WRITE = 0x1 << 1;
	PERM_INHERIT = 0x1 << 0;
)

func WriteAble(perm int32) (ret bool) {
	ret = ((perm & PERM_WRITE) == PERM_WRITE)
	return
}
func ReadAble(perm int32) (ret bool) {
	ret = ((perm & PERM_READ) == PERM_READ)
	return
}