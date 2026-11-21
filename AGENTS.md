# Project instructions

## Architecture

- Keep the project small and avoid Gradle modules without a concrete boundary.
- Model recognisers, trackers, clients, repositories, and services as objects.
- Use constructor injection for dependencies.
- Keep implementation helpers private and preserve encapsulation.
- Put platform APIs behind interfaces implemented in platform source sets.
- Keep recognition-provider data transfer objects inside the provider package.

## Kotlin style

- Use lower-case package names.
- Use UpperCamelCase type and file names.
- Use lowerCamelCase fields, functions, arguments, and local variables.
- Name test classes and files with the `Test` suffix, never `Tests`.
- Keep production tests in separate test source files.

## Product constraints

- Do not persist raw microphone audio.
- Never commit AcoustID keys, Last.fm API secrets, or user session keys.
- Follow Last.fm scrobbling rules exactly and test boundary conditions.
- Treat AcoustID viability for partial vinyl captures as an open question until
  the milestone 2 spike supplies evidence.
