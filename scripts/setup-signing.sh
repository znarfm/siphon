#!/usr/bin/env bash
set -euo pipefail

# Siphon - Android Keystore & Signing Setup Script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
KEYSTORE_PROPERTIES="${PROJECT_ROOT}/keystore.properties"

echo "=================================================="
echo "          Siphon - Signing Setup Script           "
echo "=================================================="
echo

echo "This script will help you set up release signing for Siphon."
echo "You can either generate a new keystore or use an existing one."
echo

# Step 1: Keystore source selection
echo "Select an option:"
echo "  1) Generate a new release keystore"
echo "  2) Use an existing keystore file"
read -rp "Choice [1-2] (default: 1): " CHOICE
CHOICE="${CHOICE:-1}"

KEYSTORE_PATH=""
KEY_ALIAS=""
STORE_PASSWORD=""
KEY_PASSWORD=""

if [[ "$CHOICE" == "1" ]]; then
    DEFAULT_KEYSTORE_PATH="${PROJECT_ROOT}/release.keystore"
    read -rp "Enter keystore path to generate [default: ${DEFAULT_KEYSTORE_PATH}]: " INPUT_PATH
    KEYSTORE_PATH="${INPUT_PATH:-$DEFAULT_KEYSTORE_PATH}"

    # Make absolute if relative
    if [[ "$KEYSTORE_PATH" != /* ]]; then
        KEYSTORE_PATH="${PROJECT_ROOT}/${KEYSTORE_PATH}"
    fi

    if [[ -f "$KEYSTORE_PATH" ]]; then
        echo "Warning: Keystore file already exists at ${KEYSTORE_PATH}"
        read -rp "Overwrite? (y/N): " CONFIRM_OVERWRITE
        if [[ ! "$CONFIRM_OVERWRITE" =~ ^[Yy]$ ]]; then
            echo "Aborting keystore generation."
            exit 1
        fi
        rm -f "$KEYSTORE_PATH"
    fi

    read -rp "Enter Key Alias [default: siphon]: " INPUT_ALIAS
    KEY_ALIAS="${INPUT_ALIAS:-siphon}"

    # Prompt for passwords (hidden)
    while true; do
        read -rsp "Enter Keystore Password: " STORE_PASSWORD
        echo
        if [[ -z "$STORE_PASSWORD" ]]; then
            echo "Password cannot be empty. Please try again."
            continue
        fi
        read -rsp "Confirm Keystore Password: " STORE_PASSWORD_CONFIRM
        echo
        if [[ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]]; then
            echo "Passwords do not match. Please try again."
        else
            break
        fi
    done

    read -rp "Use the same password for the key alias? (Y/n): " SAME_PASS
    SAME_PASS="${SAME_PASS:-y}"
    if [[ "$SAME_PASS" =~ ^[Yy]$ ]]; then
        KEY_PASSWORD="$STORE_PASSWORD"
    else
        while true; do
            read -rsp "Enter Key Password: " KEY_PASSWORD
            echo
            if [[ -z "$KEY_PASSWORD" ]]; then
                echo "Password cannot be empty. Please try again."
                continue
            fi
            read -rsp "Confirm Key Password: " KEY_PASSWORD_CONFIRM
            echo
            if [[ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]]; then
                echo "Passwords do not match. Please try again."
            else
                break
            fi
        done
    fi

    echo
    echo "Generating keystore with keytool..."
    keytool -genkeypair -v \
        -keystore "$KEYSTORE_PATH" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=Siphon, OU=Mobile, O=Siphon, L=Unknown, ST=Unknown, C=US"

    echo "Keystore successfully created at: $KEYSTORE_PATH"

elif [[ "$CHOICE" == "2" ]]; then
    read -rp "Enter path to existing keystore file: " INPUT_PATH
    if [[ "$INPUT_PATH" != /* ]]; then
        KEYSTORE_PATH="${PROJECT_ROOT}/${INPUT_PATH}"
    else
        KEYSTORE_PATH="$INPUT_PATH"
    fi

    if [[ ! -f "$KEYSTORE_PATH" ]]; then
        echo "Error: Keystore file not found at ${KEYSTORE_PATH}" >&2
        exit 1
    fi

    read -rp "Enter Key Alias [default: siphon]: " INPUT_ALIAS
    KEY_ALIAS="${INPUT_ALIAS:-siphon}"

    read -rsp "Enter Keystore Password: " STORE_PASSWORD
    echo
    read -rp "Is the Key Password the same as Keystore Password? (Y/n): " SAME_PASS
    SAME_PASS="${SAME_PASS:-y}"
    if [[ "$SAME_PASS" =~ ^[Yy]$ ]]; then
        KEY_PASSWORD="$STORE_PASSWORD"
    else
        read -rsp "Enter Key Password: " KEY_PASSWORD
        echo
    fi
else
    echo "Invalid option. Exiting." >&2
    exit 1
fi

echo
echo "Creating/updating ${KEYSTORE_PROPERTIES}..."

# Store relative path if under project root for portability, else absolute path
if [[ "$KEYSTORE_PATH" == "${PROJECT_ROOT}/"* ]]; then
    REL_STORE_PATH="${KEYSTORE_PATH#$PROJECT_ROOT/}"
else
    REL_STORE_PATH="$KEYSTORE_PATH"
fi

cat <<EOF > "$KEYSTORE_PROPERTIES"
storeFile=$REL_STORE_PATH
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF

chmod 600 "$KEYSTORE_PROPERTIES"
echo "Saved keystore configuration to: ${KEYSTORE_PROPERTIES} (permissions set to 600)"

# Step 2: Generate Base64
echo
echo "Encoding keystore to Base64..."
if base64 --help 2>&1 | grep -q -- "-w"; then
    KEYSTORE_BASE64=$(base64 -w 0 < "$KEYSTORE_PATH")
else
    KEYSTORE_BASE64=$(base64 < "$KEYSTORE_PATH" | tr -d '\r\n')
fi

echo
echo "=================================================="
echo "          GitHub Actions Secrets Details          "
echo "=================================================="
echo "Add the following secrets to your GitHub repository:"
echo "(Settings -> Secrets and variables -> Actions)"
echo
echo "KEYSTORE_BASE64:"
echo "${KEYSTORE_BASE64}"
echo
echo "KEYSTORE_PASSWORD:"
echo "${STORE_PASSWORD}"
echo
echo "KEY_ALIAS:"
echo "${KEY_ALIAS}"
echo
echo "KEY_PASSWORD:"
echo "${KEY_PASSWORD}"
echo "=================================================="
echo

# Step 3: GitHub CLI integration
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    echo "GitHub CLI (gh) is authenticated."
    read -rp "Would you like to automatically set these secrets on the current GitHub repository? (y/N): " SET_GH_SECRETS
    if [[ "$SET_GH_SECRETS" =~ ^[Yy]$ ]]; then
        echo "Setting GitHub secrets..."
        gh secret set KEYSTORE_BASE64 -b "${KEYSTORE_BASE64}"
        gh secret set KEYSTORE_PASSWORD -b "${STORE_PASSWORD}"
        gh secret set KEY_ALIAS -b "${KEY_ALIAS}"
        gh secret set KEY_PASSWORD -b "${KEY_PASSWORD}"
        echo "Secrets set successfully via gh CLI!"
    else
        echo "Skipped setting GitHub secrets automatically."
    fi
else
    echo "Note: GitHub CLI (gh) is not installed or not logged in. You can manually copy the secrets above to GitHub."
fi

echo
echo "=================================================="
echo "                   Summary                        "
echo "=================================================="
echo "1. Keystore: ${KEYSTORE_PATH}"
echo "2. Properties File: ${KEYSTORE_PROPERTIES}"
echo "3. .gitignore configured: keystore files and keystore.properties will NOT be committed to git."
echo "4. app/build.gradle.kts configured: release builds will automatically use this keystore."
echo
echo "Setup complete!"
