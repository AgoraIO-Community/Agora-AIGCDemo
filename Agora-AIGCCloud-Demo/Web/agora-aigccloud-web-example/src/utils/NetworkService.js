import axios from 'axios';

class NetworkService {
    constructor() {
        this.source = null;
        this.isRequesting = false;
    }

    request(config) {
        if (this.isRequesting) {
            console.warn('A request is already in progress');
            return Promise.reject(new Error('Request in progress'));
        }

        this.isRequesting = true;
        this.source = axios.CancelToken.source();

        const defaultConfig = {
            method: 'get',
            headers: {},
            data: null,
            params: null,
            cancelToken: this.source.token
        };

        const mergedConfig = { ...defaultConfig, ...config };

        return axios(mergedConfig)
            .then(response => {
                this.isRequesting = false;
                return response.data;
            })
            .catch(error => {
                this.isRequesting = false;
                if (axios.isCancel(error)) {
                    console.log('Request canceled:', error.message);
                } else {
                    console.error('Request failed:', error);
                }
                throw error;
            });
    }

    get(url, params = {}, headers = {}) {
        return this.request({ url, method: 'get', params, headers });
    }

    post(url, data = {}, headers = {}) {
        return this.request({ url, method: 'post', data, headers });
    }

    put(url, data = {}, headers = {}) {
        return this.request({ url, method: 'put', data, headers });
    }

    delete(url, params = {}, headers = {}) {
        return this.request({ url, method: 'delete', params, headers });
    }

    stop() {
        if (this.source) {
            this.source.cancel('Operation canceled by the user.');
            this.isRequesting = false;
        }
    }
}

export default new NetworkService();