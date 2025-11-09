const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function getGitVersion() {
    // Check if VERSION is provided as environment variable (used in Docker builds)
    if (process.env.VERSION) {
        console.log(`Using VERSION from environment: ${process.env.VERSION}`);
        return process.env.VERSION;
    }

    // Try to get version from git
    try {
        const version = execSync('git describe --tags --always --dirty', { encoding: 'utf-8' }).trim();
        return version || '0.0.1-SNAPSHOT';
    } catch (error) {
        console.warn('Unable to get Git version, using default: 0.0.1-SNAPSHOT');
        return '0.0.1-SNAPSHOT';
    }
}

function generateVersionFile() {
    const version = getGitVersion();
    const versionFilePath = path.join(__dirname, '..', 'src', 'environments', 'version.ts');

    const content = `// This file is auto-generated. Do not edit manually.
// Generated on: ${new Date().toISOString()}

export const VERSION = '${version}';
`;

    fs.writeFileSync(versionFilePath, content, 'utf-8');
    console.log(`Version file generated: ${version}`);
}

generateVersionFile();