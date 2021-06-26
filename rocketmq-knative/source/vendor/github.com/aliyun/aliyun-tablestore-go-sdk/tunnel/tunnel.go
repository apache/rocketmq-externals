package tunnel

type TunnelClient interface {
	TunnelMetaApi
	NewTunnelWorker(tunnelId string, workerConfig *TunnelWorkerConfig) (TunnelWorker, error)
}

type DefaultTunnelClient struct {
	api *TunnelApi
}

func NewTunnelClient(endpoint, instanceName, accessId, accessKey string) TunnelClient {
	return NewTunnelClientWithConfig(endpoint, instanceName, accessId, accessKey, nil)
}

func NewTunnelClientWithConfig(endpoint, instanceName, accessId, accessKey string, conf *TunnelConfig) TunnelClient {
	return &DefaultTunnelClient{
		api: NewTunnelApi(endpoint, instanceName, accessId, accessKey, conf),
	}
}

func NewTunnelClientWithToken(endpoint, instanceName, accessId, accessKey, token string, conf *TunnelConfig) TunnelClient {
	return &DefaultTunnelClient{
		api: NewTunnelApiWithToken(endpoint, instanceName, accessId, accessKey, token, conf),
	}
}

func (c *DefaultTunnelClient) CreateTunnel(req *CreateTunnelRequest) (*CreateTunnelResponse, error) {
	return c.api.CreateTunnel(req)
}

func (c *DefaultTunnelClient) DeleteTunnel(req *DeleteTunnelRequest) (*DeleteTunnelResponse, error) {
	return c.api.DeleteTunnel(req)
}

func (c *DefaultTunnelClient) ListTunnel(req *ListTunnelRequest) (*ListTunnelResponse, error) {
	return c.api.ListTunnel(req)
}

func (c *DefaultTunnelClient) DescribeTunnel(req *DescribeTunnelRequest) (*DescribeTunnelResponse, error) {
	return c.api.DescribeTunnel(req)
}

func (c *DefaultTunnelClient) GetRpo(req *GetRpoRequest) (*GetRpoResponse, error) {
	return c.api.GetRpo(req)
}

func (c *DefaultTunnelClient) Schedule(req *ScheduleRequest) (*ScheduleResponse, error) {
	return c.api.Schedule(req)
}

func (c *DefaultTunnelClient) NewTunnelWorker(tunnelId string, workerConfig *TunnelWorkerConfig) (TunnelWorker, error) {
	if workerConfig == nil {
		return nil, &TunnelError{Code: ErrCodeClientError, Message: "TunnelWorkerConfig can not be nil"}
	}
	conf := *workerConfig
	return newTunnelWorker(tunnelId, c.api, &conf)
}
