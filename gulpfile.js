'use strict';
//1、载入外挂
var gulp = require('gulp'),
    autoprefixer = require('gulp-autoprefixer'),//自动补全css外挂，并根据W3C规范自动适应多浏览器
    minifyCss = require('gulp-minify-css'),
    uglify = require('gulp-uglify'),
    imagemin = require('gulp-imagemin'),
    clean = require('gulp-clean'),
    concat = require('gulp-concat'),
    notify = require('gulp-notify'),
    cache = require('gulp-cache'),
    changed = require('gulp-changed'),
    rev = require('gulp-rev'),//生成版本 hash 的静态文件
    minifyHTML = require('gulp-minify-html'),
    revReplace = require('gulp-rev-replace'),
    sftp = require('gulp-sftp'),
    zip = require('gulp-zip'),
    connect  = require('gulp-connect');

//清理构建临时文件
gulp.task('clean', function () {
    return gulp.src(['src/main/webapp/src','src/main/webapp/style','src/main/webapp/vendor','src/main/webapp/view','src/main/webapp/web-resource','src/main/webapp/*.html','dist'], {read: false})
        .pipe(clean())
//        .pipe(notify({ message: 'clean task complete' }));
})

gulp.task('copy',['clean'], function () {
    return gulp.src(['dev/**/*','!dev/**/*.js','!dev/**/*.css','!dev/**/*.html'])
        .pipe(gulp.dest('src/main/webapp'))
//        .pipe(notify({ message: 'copy font task complete' }));
});

gulp.task('concat', ['clean'],function () {
    gulp.src('dev/**/*.js')  //要合并的文件
        .pipe(concat('all.js'))  // 合并匹配到的js文件并命名为 "all.js"
        .pipe(gulp.dest('src/main/webapp'))
});

gulp.task('css',['clean'], function () {
    return gulp.src(['dev/**/*.css'])
        .pipe(minifyCss())                                      //- 压缩处理成一行
        .pipe(rev())                                            //- 文件名加MD5后缀
        .pipe(gulp.dest('src/main/webapp'))                               //- 输出文件本地
        .pipe(rev.manifest('rev-manifest-css.json'))                                   //- 生成一个 rev-manifest.json
        .pipe(gulp.dest('dist'));                              //- 将 rev-manifest.json 保存到 rev 目录内
//        .pipe(notify({ message: 'Scripts task complete' }));
});

gulp.task('scripts',['clean'], function () {
    return gulp.src(['dev/**/*.js'])
        .pipe(uglify({mangle:false}))                       //- 不能修改变量，部分代码不规范，需要优化
        .pipe(rev())
        .pipe(gulp.dest('src/main/webapp'))
        .pipe(rev.manifest('rev-manifest.json'))
        .pipe(gulp.dest('dist'))
//        .pipe(notify({ message: 'Scripts task complete' }));
});

//压缩修改名称
gulp.task('build',['scripts','css','copy'], function () {
    var manifest = gulp.src(["dist/rev-manifest.json",'dist/rev-manifest-css.json']);
    return gulp.src(['dev/**/*.html'])
        .pipe(revReplace ({manifest: manifest}))
        .pipe(minifyHTML({
            conditionals:true
        }))
        .pipe(gulp.dest('src/main/webapp'))
//        .pipe(notify({ message: 'rev task complete' }));
});

//使用connect启动一个Web服务器
gulp.task('connect', function () {
    connect.server({
        root: 'dist'
    });
});

gulp.task('copyAll', function () {
    return gulp.src(['dev/**/*'])
        .pipe(changed('dev/**/*'))
        .pipe(gulp.dest('src/main/webapp'))
//        .pipe(notify({ message: 'copy font task complete' }));
});

gulp.task('reload', function () {
    gulp.src('dev/**/*')
        .pipe(connect.reload());
});

gulp.task('watch', function () {
    gulp.watch(['dev/**/*'], ['copyAll']);
});

gulp.task('default', ['watch','copyAll']);