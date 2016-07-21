# This file has been automatically generated, please do not modify directly.
load("//tools/base/bazel:bazel.bzl", "kotlin_library", "groovy_library", "kotlin_groovy_library", "fileset")

java_library(
  name = "sherpa-solver",
  srcs = glob([
      "solver/src/main/java/**/*.java",
    ]),
  resource_strip_prefix = "tools/sherpa/sherpa-solver.resources",
  resources = [
      "//tools/sherpa:sherpa-solver.res",
    ],
  deps = [
      "@local_jdk//:langtools-neverlink",
    ],
  javacopts = ["-extra_checks:off"],
  visibility = ["//visibility:public"],
)

fileset(
  name = "sherpa-solver.res",
  srcs = glob([
      "solver/src/main/java/**/*",
    ],
    exclude = [
      "**/* *",
      "**/*.java",
      "**/*.kt",
      "**/*.groovy",
      "**/*$*",
      "**/.DS_Store",
    ]),
  mappings = {
      "solver/src/main/java": "sherpa-solver.resources",
    },
  deps = [
      "@local_jdk//:langtools-neverlink",
    ],
)
