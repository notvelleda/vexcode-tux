const vscode = require('vscode');
const fs = require('fs');
const os = require('os');
const child_process = require('child_process');

function findProjectFile() {
	if (vscode.workspace.workspaceFolders === undefined) return undefined;
	for (let i = 0; i < vscode.workspace.workspaceFolders.length; i ++) {
		let folder = vscode.workspace.workspaceFolders[i];
		let files = fs.readdirSync(folder.uri.path);
		for (let j = 0; j < files.length; j ++) {
			let file = files[j];
			if (file.endsWith('.v5code')) {
				return folder.uri.path + '/' + file;
			}
		}
	}
}

function findStorageDir() {
	let platform = process.platform;
	let path = '';
	if (platform === 'linux' || platform === 'freebsd' || platform === 'openbsd') {
		path = os.homedir() + '/.vexcodetux';
	} else if (platform === 'darwin') {
		path = os.homedir() + '/Library/Application Support/vexcodetux';
	} else if (platform === 'win32') {
		path = process.env.APPDATA === undefined ? os.homedir() + '/.vexcodetux' : process.env.APPDATA + '/.vexcodetux';
	} else {
		path = os.homedir() + '/vexcodetux';
	}
	return path;
}

function findJarFile() {
	let path = findStorageDir();
	if (fs.existsSync(path + '/vexcodetux.jar')) {
		return path + '/vexcodetux.jar';
	} else {
		return undefined;
	}
}

function errorMissingJAR(channel) {
	vscode.window.showErrorMessage('Could not find the VEXCode Tux JAR file!');
	channel.appendLine('Could not find the VEXCode Tux JAR file!');
	channel.appendLine('Expected file to be ' + findStorageDir() + '/vexcodetux.jar');
}

function errorMissingProject(channel) {
	vscode.window.showErrorMessage('Could not find a compatible project file!');
	channel.appendLine('Could not find a VEXCode V5 Text-compatible project file!');
	channel.appendLine('Make sure you have a folder open containing the project file (.v5code) and try again.');
}

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
	let channel = vscode.window.createOutputChannel("VEXCode Tux Build Result");

	let buildProject = vscode.commands.registerCommand('vexcode-tux.buildProject', function () {
		let projectFile = findProjectFile();
		let jarLocation = findJarFile();
		if (jarLocation === undefined) {
			errorMissingJAR(channel);
			return;
		} else if (projectFile === undefined) {
			errorMissingProject(channel);
			return;
		} else {
			vscode.window.showInformationMessage('Building project...');
			channel.appendLine('java -jar ' + jarLocation + ' -pb ' + projectFile);

			const proc = child_process.spawn('java', ['-jar', jarLocation, '-pb', projectFile]);

			proc.stdout.on('data', (data) => {
				channel.append(data.toString());
			});

			proc.stderr.on('data', (data) => {
				channel.append(data.toString());
			});

			proc.on('close', (code) => {
				if (code === 0) {
					vscode.window.showInformationMessage('Successfully built project.');
				} else {
					vscode.window.showErrorMessage('Build failed! See output for details.');
				}
			});
		}
	});

	let uploadProject = vscode.commands.registerCommand('vexcode-tux.uploadProject', function () {
		let projectFile = findProjectFile();
		let jarLocation = findJarFile();
		if (jarLocation === undefined) {
			errorMissingJAR(channel);
			return;
		} else if (projectFile === undefined) {
			errorMissingProject(channel);
			return;
		} else {
			vscode.window.showInformationMessage('Uploading project...');
			channel.appendLine('java -jar ' + jarLocation + ' -pbu ' + projectFile);

			const proc = child_process.spawn('java', ['-jar', jarLocation, '-pbu', projectFile]);

			proc.stdout.on('data', (data) => {
				channel.append(data.toString());
			});

			proc.stderr.on('data', (data) => {
				channel.append(data.toString());
			});

			proc.on('close', (code) => {
				if (code === 0) {
					vscode.window.showInformationMessage('Successfully uploaded project.');
				} else {
					vscode.window.showErrorMessage('Upload failed! See output for details.');
				}
			});
		}
	});

	let openProjectGUI = vscode.commands.registerCommand('vexcode-tux.openProjectGUI', function () {
		let projectFile = findProjectFile();
		let jarLocation = findJarFile();
		if (jarLocation === undefined) {
			errorMissingJAR(channel);
			return;
		} else if (projectFile === undefined) {
			errorMissingProject(channel);
			return;
		} else {
			vscode.window.showInformationMessage('Opening project...');
			channel.appendLine('java -jar ' + jarLocation + ' -pg ' + projectFile);

			child_process.spawn('java', ['-jar', jarLocation, '-pg', projectFile], {
				detached: true
			});
		}
	});

	let openGUI = vscode.commands.registerCommand('vexcode-tux.openGUI', function () {
		let jarLocation = findJarFile();
		if (jarLocation === undefined) {
			errorMissingJAR(channel);
			return;
		} else {
			vscode.window.showInformationMessage('Opening GUI...');
			channel.appendLine('java -jar ' + jarLocation);

			child_process.spawn('java', ['-jar', jarLocation], {
				detached: true
			});
		}
	});

	context.subscriptions.push(buildProject);
	context.subscriptions.push(uploadProject);
	context.subscriptions.push(openProjectGUI);
	context.subscriptions.push(openGUI);
}
exports.activate = activate;

function deactivate() {}

module.exports = {
	activate,
	deactivate
};
