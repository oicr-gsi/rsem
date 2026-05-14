# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.2] - 2026-05-13
### Added
- [GRD-1175](https://jira.oicr.on.ca/browse/GRD-1175)
- Added gencode as a required workflow input to support gencode version selection via olive assay_confirguration.
  
### Changed
- Replaced hardcoded gencode version in module with a dinamic lookup via nested Map

## [1.1.1] - 2025-09-25
### Changed
- [GRD-964](https://jira.oicr.on.ca/browse/GRD-964)
- Update workflow hg38 genome to genecode 44 

## [1.1.0] - 2024-06-28
### Added
- [GRD-797](https://jira.oicr.on.ca/browse/GRD-797) - Add vidarr labels to outputs (changes to medata only).

## [Unreleased] - 2021-11-11
### Fixed
- [GP-2885](https://jira.oicr.on.ca/browse/GP-2885) Make RT tests more robust.

## [1.0.1] - 2021-05-31
### Changed
- Migration to vidarr. 
- [GP-2734](https://jira.oicr.on.ca/browse/GP-2734)

## [Unreleased] - 2020-06-01
### Changed
- Conversion to wdl, removing ngsutils as this dependency is no longer maintained.

## [Unreleased] - 2017-06-28
### Added
- Added flag to control provisioning of RSEM bam file.

## [1.0.0] - 2016-10-11
### Added
- Initial import of RSEM code.