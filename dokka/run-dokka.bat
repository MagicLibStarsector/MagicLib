java -jar dokka-cli-1.8.10.jar \
     -pluginsClasspath "./dokka-base-1.8.10.jar;./dokka-analysis-1.8.10.jar;./kotlin-analysis-intellij-1.8.10.jar;./kotlin-analysis-compiler-1.8.10.jar;./kotlinx-html-jvm-0.8.0.jar;./freemarker-2.3.31.jar" \
     -sourceSet "-src ../../src/main/java" \
     -outputDir "./dokka/html"

pause