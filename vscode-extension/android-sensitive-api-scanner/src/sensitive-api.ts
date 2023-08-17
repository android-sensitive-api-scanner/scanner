interface SensitiveAPI {
    owner: String;
    name: String;
    descriptor: String;
}

interface SensitiveAPIs {
    [index: number]: SensitiveAPI
}