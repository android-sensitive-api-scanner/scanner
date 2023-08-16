// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import * as path from "path";
import * as fs from "fs";
import * as shell from "./shell";

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {

	// Use the console to output diagnostic information (console.log) and errors (console.error)
	// This line of code will only be executed once when your extension is activated
	console.log('Congratulations, your extension "android-sensitive-api-scanner" is now active!');

	// The command has been defined in the package.json file
	// Now provide the implementation of the command with registerCommand
	// The commandId parameter must match the command field in package.json
	let disposable = vscode.commands.registerCommand('android-sensitive-api-scanner.openAPK', async () => {
		const uris = await vscode.window.showOpenDialog({
			openLabel: "Select",
			canSelectFiles: true,
			canSelectFolders: false,
			canSelectMany: false,
			filters: {
				apk: ["apk"]
			}
		});
		if (uris) {
			const apkPath = uris[0].fsPath;
			let outputDir = path.join(
				path.dirname(apkPath),
				path.parse(apkPath).name,
			);
			let apiFile = path.join(
				outputDir,
				"sensitive-apis.json"
			);

			if (!fs.existsSync(outputDir)) {
				fs.mkdirSync(outputDir, { recursive: true });
			}
			if (fs.existsSync(apiFile)) {
				fs.rmSync(apiFile);
			}

			vscode.window.showInformationMessage(`output dir: ${outputDir}`);
			vscode.window.showInformationMessage(`api file: ${apiFile}`);


			const configuration = vscode.workspace.getConfiguration();
			const jarPath = configuration.get("jarPath");
			const sensitiveAPIs = configuration.get<SensitiveAPIs>("sensitiveAPIs");

			if (sensitiveAPIs) {
				fs.writeFileSync(apiFile, JSON.stringify(sensitiveAPIs));

				let output = await vscode.window.withProgress({
					location: vscode.ProgressLocation.Notification,
					title: "",
					cancellable: false,
				}, (progress, token) => {
					token.onCancellationRequested(() => {
						console.log("User canceled the long running operation");
					});
					progress.report({message: "Scanning... Please wait a moment."});
					return shell.exec(`java -jar ${jarPath} -json ${apiFile} --output-dir ${outputDir} ${apkPath}`);
				});
				
				const outputArr = output.split("\n").filter((e) => {
					return e !== '' && e !== undefined;
				});
				const outputFile = outputArr[outputArr.length - 1];
				const document = await vscode.workspace.openTextDocument(outputFile);
				vscode.window.showTextDocument(document);
			}

		}
	});

	context.subscriptions.push(disposable);
}

// This method is called when your extension is deactivated
export function deactivate() { }
