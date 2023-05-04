package com.satya.projectanalysis;

import org.apache.commons.io.FileUtils;
import spoon.reflect.code.CtInvocation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

public class RegexBasedAnalysis {
    public static void main(String[] args) {

    }

    private static String inferLombokBuildMethod(String s) {
        return extractUsingRegex(s, "(.+)\\.builder\\(\\)\\..*\\.build\\(\\)", 1);
    }

    private static String extractUsingRegex(String string, String regex, Integer groupNumber) {
        Matcher matcher = Pattern.compile(regex).matcher(string);
        if (matcher.find()) {
            return matcher.group(groupNumber);
        }
        return null;
    }


    public static MethodInvocationProcessor.ArgumentDetail tryToInferType(CtInvocation invocation) {
        return ofNullable(inferLombokBuildMethod(invocation.toString()))
                .map(typeName -> getQualifiedTypeName(invocation, typeName))
                .map(typeName -> MethodInvocationProcessor.ArgumentDetail.builder().type(typeName).build()).orElse(null);
    }

    private static String getQualifiedTypeName(CtInvocation invocation, String typeName) {
        try {
            List<String> lines = FileUtils.readLines(invocation.getPosition().getCompilationUnit().getFile(), Charset.defaultCharset());
            return lines.stream().map(ln -> getQualifiedNameFromImportStatement(ln, typeName)).filter(Objects::nonNull).findFirst().orElse(typeName);
        } catch (IOException e) {
            e.printStackTrace();
            return typeName;
        }
    }

    private static String getQualifiedNameFromImportStatement(String ln, String typeName) {
        return ofNullable(extractUsingRegex(ln.trim(), "import (.+)\\." + typeName + "\\s*;", 1))
                .map(it -> it + "." + typeName)
                .orElse(null);
    }
}
