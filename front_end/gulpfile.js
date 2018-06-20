var gulp = require("gulp");
var sourcemaps = require("gulp-sourcemaps");
var babel = require("gulp-babel");
var concat = require("gulp-concat");
var uglify = require("gulp-uglify");
var cleanCSS = require("gulp-clean-css");
var ngAnnotate = require("gulp-ng-annotate")
var htmlmin = require("gulp-htmlmin");
var del = require("del");

var paths = {
  scripts: "javascripts/**/*",
  html: "html/**/*",
  bower_components_js: ["bower_components/jquery/dist/jquery.slim.min.js"
		      , "bower_components/angular/angular.min.js"
		      , "bower_components/popper.js/dist/umd/popper.min.js"
		      , "bower_components/bootstrap/dist/js/bootstrap.min.js"
		      , "bower_components/angular-xeditable/dist/js/xeditable.min.js"
		      , "bower_components/angular-bootstrap/ui-bootstrap-tpls.min.js"],
  bower_components_css: ["bower_components/bootstrap/dist/css/bootstrap.min.css"
        	       , "bower_components/angular-xeditable/dist/css/xeditable.min.css"],
  bower_components_fonts: "bower_components/bootstrap/dist/fonts/*",
  styles: "stylesheets/**/*",
  img: "images/**/*",
  root: "../public",  
  js: "../public/javascripts",
  css: "../public/stylesheets"
};

gulp.task("clean", function(cb) {
  return del([paths.root + "/*"
	    , paths.js + "/*"
	    , paths.css + "/*"
	   , paths.root + "/images/*"], {force: true}, cb);
});


gulp.task("lib_css", ["clean"], function () {
  return gulp.src(paths.bower_components_css)
    .pipe(gulp.dest(paths.css + "/lib"));
});

gulp.task("lib_js", ["clean"], function () {
  return gulp.src(paths.bower_components_js)
    .pipe(gulp.dest(paths.js + "/lib"));
});

gulp.task("css", ["clean"], function () {
  return gulp.src(paths.styles)
    .pipe(sourcemaps.init())
    .pipe(concat("app.css"))
    .pipe(cleanCSS({ format: 'beautify'}))
    .pipe(sourcemaps.write("."))
    .pipe(gulp.dest(paths.css));
});

gulp.task("fonts", ["clean"], function () {
  return gulp.src(paths.bower_components_fonts)
    .pipe(gulp.dest(paths.css + "/fonts"));
});


gulp.task("js", ["clean"], function () {
  return gulp.src(["javascripts/app.module.js", paths.scripts])
    .pipe(sourcemaps.init())
    .pipe(babel()) 
    .pipe(concat("app.js"))
    .pipe(ngAnnotate())	
    .pipe(uglify({mangle: false}))
    .pipe(sourcemaps.write("."))
    .pipe(gulp.dest(paths.js));
});

gulp.task("img", ["clean"], function () {
  return gulp.src(paths.img)
    .pipe(gulp.dest(paths.root + "/images"));
});

gulp.task("html", ["clean"], function () {
  return gulp.src(paths.html)
    .pipe(htmlmin({collapseWhitespace: true}))
    .pipe(gulp.dest(paths.root));
});


gulp.task("default", ["clean", "lib_css", "lib_js", "css", "fonts", "js", "img", "html"]);
