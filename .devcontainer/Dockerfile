FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

# The sshd feature runs apt-get update, which fails on the base image's
# unused Yarn repository because its signing key is missing. Bullseye's
# security mirror can also publish package indexes before the referenced
# package archives are available, so use Bookworm instead.
RUN rm -f /etc/apt/sources.list.d/yarn.list

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3-venv libgtk-3-0 xvfb xauth \
    && python3 -m venv /opt/apm \
    && if ! /opt/apm/bin/pip install --no-cache-dir apm-cli==0.34.0; then \
        echo 'Could not install APM; see CONTRIBUTING.md for recovery instructions.' >&2; \
    fi \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/opt/apm/bin:${PATH}"
