# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-08-20

### Added

#### Agent SDK (`agent/agent-sdk/`)
- Privacy core APIs: MaskingApi, DpApi, KAnonymityApi, QolApi, LocalDpApi
- DynClassification engine: three-layer funnel (Rule → NER → LLM)
- YAML-based rule loading with Jackson snake_case support
- PrivacyClient builder pattern
- PrivacyProfile configuration model

#### Agent Server (`agent/agent-server/`)
- gRPC service implementation: 33 RPC methods
- REST controllers: 13 endpoints (Mask, DP, KAnon, QoL, DynClassify, Medical)
- PrivacyService orchestration layer
- Spring Boot 3.3.5 + gRPC 1.62.2 integration
- Health check endpoint
- Actuator metrics

#### Console (`console/`)
- Go backend: gRPC proxy + REST forwarding
- React frontend: Vite + TailwindCSS + TypeScript
- 18 UI components (BackendSelector, DynClassificationPanel, MedicalPipelinePanel, etc.)
- Start scripts for dev/prod/docker modes

#### Privacy Java SDK (`privacy-java-sdk/`)
- Industrial-grade privacy computation library
- P0: SLF4J logging, GitHub Actions CI, JaCoCo coverage, LICENSE
- P1: Checkstyle/SpotBugs, immutable returns, integration tests
- P2: Builder pattern, Micrometer metrics, JMH benchmarks
- 80 unit/integration/property tests passing

#### Documentation (`docs/`)
- Architecture design & summary
- Module docs: masking, dp, k_anonymity, qol, dynclassification, medical_pipeline
- Deployment guide
- Standard references (GB/T 43697, JR/T 0197)

### Infrastructure
- Apache License 2.0
- .gitignore for Maven/IDE/OS artifacts
- Dockerfile for agent containerization
- README.md with full API examples
