# android-sensitive-api-scanner

这是一个命令行工具，用于针对 apk 文件进行静态扫描方法引用链。配置想要被扫描的方法请见[这里](https://github.com/porum/android-sensitive-api-scanner/blob/master/scanner/src/test/resources/sensitive-api.json)。类图的构建基于 [jadx](https://github.com/skylot/jadx) 。

## Requirements

- #### JDK11+

## Usage
```
Usage: java -jar android-sensitive-api-scanner.jar [<options>] <apk>

Options:
  --apis=<text>     sensitive apis json file
  --output=<text>   output directory
  --mapping=<text>  mapping file
  -h, --help        Show this message and exit

Arguments:
  <apk>  apk file
```

## Example

在命令行输入：

```shell
java -jar android-sensitive-api-scanner.jar demo.apk -apis sensitive-apis.json -output out/
```

稍等片刻（p.s. 扫描时间的快慢取决于被扫描的 apk 的工程代码量）后，即可生成输出结果，内容形如：

```
mecox.b.b.()V
   mecox.b.b.()Z
      mecox.b.b.()V
         mecox.a.a.()Z
            mecox.a.a.()Z
               android.os.Build.BOARD:Ljava/lang/String;
```

如果想扫描自己工程的 apk，可以指定 mapping 文件，这样扫描出来的结果就是反混淆后的原始代码。
