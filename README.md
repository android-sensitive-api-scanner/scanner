# android-sensitive-api-scanner

这是一个针对 apk 文件进行静态扫描敏感权限引用链的一个库。类图的构建基于 [jadx](https://github.com/skylot/jadx) 。

## Usage
```
java -jar asas.jar [options] <input files> (.apk)
options:
  -json                               - sensitive api config
  -mapping                            - mapping file
  -d, --output-dir                    - output directory

Examples:
  java -jar asas.jar -json sensitive-api-config.json -mapping mapping.txt -d out sample.apk
```

sensitive api config format look at [here](https://github.com/porum/android-sensitive-api-scanner/blob/master/scanner/src/test/resources/sensitive-api.json).