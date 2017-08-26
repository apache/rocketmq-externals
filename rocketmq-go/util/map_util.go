package util

import (
	"reflect"
	"strings"
)

//Struct2Map convert interface{} to map[string]interface{}
func Struct2Map(structBody interface{}) (resultMap map[string]interface{}) {
	resultMap = make(map[string]interface{})
	value := reflect.ValueOf(structBody)
	for value.Kind() == reflect.Ptr {
		value = value.Elem()
	}
	if value.Kind() != reflect.Struct {
		panic("input is not a struct")
	}
	valueType := value.Type()
	for i := 0; i < valueType.NumField(); i++ {
		field := valueType.Field(i)
		if field.PkgPath != "" {
			continue
		}
		name := field.Name
		smallName := strings.Replace(name, string(name[0]), string(strings.ToLower(string(name[0]))), 1)
		val := value.FieldByName(name).Interface()
		resultMap[smallName] = val
	}
	return
}
