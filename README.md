# diffy-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fdiffy--kotlin-blue.svg)](https://github.com/KotlinMania/diffy-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/diffy-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/diffy-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/diffy-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/diffy-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`bmwill/diffy`](https://github.com/bmwill/diffy).

**Original Project:** This port is based on [`bmwill/diffy`](https://github.com/bmwill/diffy). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `bmwill/diffy`

> The text below is reproduced and lightly edited from [`https://github.com/bmwill/diffy`](https://github.com/bmwill/diffy). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## diffy

[![diffy on crates.io](https://img.shields.io/crates/v/diffy)](https://crates.io/crates/diffy)
[![Documentation (latest release)](https://docs.rs/diffy/badge.svg)](https://docs.rs/diffy/)
[![Documentation (master)](https://img.shields.io/badge/docs-master-59f)](https://bmwill.github.io/diffy/diffy/)
[![License](https://img.shields.io/badge/license-Apache-green.svg)](LICENSE-APACHE)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE-MIT)

Tools for finding and manipulating differences between files

## License

This project is available under the terms of either the [Apache 2.0
license](https://github.com/bmwill/diffy/blob/HEAD/LICENSE-APACHE) or the [MIT license](https://github.com/bmwill/diffy/blob/HEAD/LICENSE-MIT).

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:diffy-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`bmwill/diffy`](https://github.com/bmwill/diffy). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the diffy authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`bmwill/diffy`](https://github.com/bmwill/diffy) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
