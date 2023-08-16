import * as child_process from "child_process";

export const exec = (cmd: string) =>
    new Promise<string>((resolve, reject) => {
        child_process.exec(cmd, (err, out) => {
            if (err) {
                return reject(err);
            }
            return resolve(out);
        });
    });