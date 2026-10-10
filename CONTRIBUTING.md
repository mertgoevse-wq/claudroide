# Contributing to Claudroide Next

Thank you for contributing to Claudroide Next.

## Guidelines

1. **Own Code & Third-Party Code:**
   - Original code created for Claudroide Next requires no attribution headers.
   - Borrowed code, libraries, or ported components must explicitly cite the original author, upstream repository, and license at the top of the file.

2. **Security & Secrets:**
   - Never commit API keys, personal access tokens, private keys, or passwords.
   - All commits must pass `python3 tools/secret_gate.py .` before being accepted.

3. **Verifiable Testing:**
   - All code contributions must be accompanied by unit tests.
   - Every build must pass `./gradlew :app:testDebugUnitTest`.

4. **Code Style & Architectural Invariants:**
   - Follow established Kotlin idioms and Jetpack Compose patterns.
   - Respect concurrency and device resource limits: strict separation of responsibilities, no blocking background loops without cancellation support.
