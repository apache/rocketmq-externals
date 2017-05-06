package service

type ConsumeFromWhere int

func (c ConsumeFromWhere) String() string {
	switch c {
	case ConsumeFromLastOffset:
		return "ConsumeFromLastOffset"
	case ConsumeFromFirstOffset:
		return "ConsumeFromFirstOffset"
	case ConsumeFromTimestamp:
		return "ConsumeFromTimestamp"
	}
	return "unknow ConsumeFromWhere"
}

const (
	ConsumeFromLastOffset ConsumeFromWhere = iota
	ConsumeFromFirstOffset
	ConsumeFromTimestamp
)

type ConsumeType string

const (
	ConsumeActively  ConsumeType = "PULL"
	ConsumePassively ConsumeType = "PUSH"
)

type MessageModel int

const (
	Broadcasting MessageModel = iota
	Clustering
)

func (m MessageModel) String() string {
	switch m {
	case Broadcasting:
		return "MessageModel"
	case Clustering:
		return "Running"
	}
	return "unknow MessageModel"
}
