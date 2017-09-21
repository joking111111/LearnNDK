# LearnNDK
## 集成增量更新的检查更新module
### 1.自动更新module是在github某项目基础上修改的，主要拦截了下载完成后安装过程
    通过判断下载url的后缀名是否是.patch还是.apk，从而选择全量更新还是增量更新。
    此外，修改了原项目的通信方式，改为本地广播；控制好Service的生命周期；针对Context预防内存泄漏；检查更新新增方法，以区分是否弹提示已经是最新版。
    
### 2.参考项目地址https://github.com/MZCretin/AutoUpdateProject

### 3.用法
    1.与后台开发人员约束数据格式参考上述项目地址。
    
    2.参考module app中Application和MainActivity的用法。
