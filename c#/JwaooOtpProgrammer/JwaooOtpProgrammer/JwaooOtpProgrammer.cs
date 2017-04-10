﻿using System;
using System.IO;
using System.Collections.Generic;
using System.Text;
using System.Windows.Forms;
using System.Diagnostics;
using System.Configuration;
using System.Threading;

namespace JwaooOtpProgrammer {

    public partial class JwaooOtpProgrammer : Form {

        private const String KEY_SMATR_SNIPPETS_PATH = "SmartSnippet";
        private const String KEY_FIRMWARE_PATH = "Firmware";

        private static String[] sProgramFiles = {
            "C:\\Program Files (x86)", "C:\\Program Files", "D:\\Program Files (x86)", "D:\\Program Files"
        };

        private static String sFileOtpHeaderBin = Path.Combine(Application.LocalUserAppDataPath, "otp_header.bin");
        private static String sFileOtpFirmwareBin = Path.Combine(Application.LocalUserAppDataPath, "otp_firmware.bin");

        private static String sFileProgrammerBin = Path.Combine(Application.StartupPath, "jtag_programmer.bin");
        private static String[] sOtpCommandArgs = { "-type", "otp", "-chip", "DA14580-01", "-jtag", "123456", "-baudrate", "57600" };

        private static byte[] sOtpBootMagic = { 0xA5, 0xA5, 0x34, 0x12, 0x34, 0x12, 0xA5, 0xA5 };

        private String mFileSmartSnippetsExe;
        private FileStream mFileStreamLog;
        private JwaooMacAddress mMacAddress;
        private bool mBurnSuccess;

        private JwaooMacAddress[] mMacAddressArray = {
            new JwaooMacAddress("JwaooMacModel06.txt", "JwaooFwModel06", new CavanMacAddress().fromString("88:EA:00:00:00:00")),
            new JwaooMacAddress("JwaooMacModel10.txt", "JwaooFwModel10", new CavanMacAddress().fromString("88:EB:00:00:00:00")),
        };

        public JwaooOtpProgrammer() {
            InitializeComponent();

            loadConfigFile();
            openFileDialogFirmware.InitialDirectory = Application.StartupPath;
            openFileDialogSmartSnippets.InitialDirectory = Application.StartupPath;
        }

        private void Programmer_FormClosed(object sender, FormClosedEventArgs e) {
            if (mFileStreamLog != null) {
                mFileStreamLog.Close();
                mFileStreamLog = null;
            }

            saveConfigFile();
        }

        private JwaooMacAddress getMacAddress(String pathname) {
            String name = Path.GetFileName(pathname);

            foreach (JwaooMacAddress address in mMacAddressArray) {
                if (name.StartsWith(address.FwPrefix)) {
                    return address;
                }
            }

            return null;
        }

        private bool setFwFilePath(String pathname) {
            JwaooMacAddress address = getMacAddress(pathname);
            if (address == null) {
                MessageBox.Show("固件文件名不正确，请重新选择！");
                return false;
            }

            mMacAddress = address;
            readMacAddressFromFile(true);

            textBoxFirmware.Text = pathname;
            buttonAddressEdit.Enabled = true;
            buttonAddressAlloc.Enabled = true;

            return true;
        }

        public void loadConfigFile() {
            String pathname = ConfigurationManager.AppSettings[KEY_FIRMWARE_PATH];
            if (File.Exists(pathname)) {
                setFwFilePath(pathname);
            }

            pathname = ConfigurationManager.AppSettings[KEY_SMATR_SNIPPETS_PATH];
            if (File.Exists(pathname)) {
                mFileSmartSnippetsExe = pathname;
            }
        }

        public bool saveConfigFile() {
            try {
                Configuration config = ConfigurationManager.OpenExeConfiguration(Application.ExecutablePath);
                if (config == null) {
                    return false;
                }

                KeyValueConfigurationCollection settings = config.AppSettings.Settings;
                settings.Clear();

                if (File.Exists(mFileSmartSnippetsExe)) {
                    settings.Add(KEY_SMATR_SNIPPETS_PATH, mFileSmartSnippetsExe);
                }

                String pathname = textBoxFirmware.Text;
                if (File.Exists(pathname)) {
                    settings.Add(KEY_FIRMWARE_PATH, pathname);
                }

                config.Save(ConfigurationSaveMode.Full);

                return true;
            } catch (Exception e) {
                MessageBox.Show("保存配置出错: " + e);
                return false;
            }
        }

        public bool writeLogFile(String text) {
            if (!checkBoxSaveLog.Checked) {
                return true;
            }

            if (mFileStreamLog == null) {
                mFileStreamLog = File.Open(Path.Combine(Application.StartupPath, "log.txt"), FileMode.Append, FileAccess.Write, FileShare.Read);
                if (mFileStreamLog == null) {
                    return false;
                }
            }

            try {
                byte[] bytes = Encoding.UTF8.GetBytes(text);
                mFileStreamLog.Write(bytes, 0, bytes.Length);
                mFileStreamLog.Flush();
                return true;
            } catch {
                return false;
            }
        }

        public bool writeLog(String line) {
            return writeLogFile(line) && writeLogFile("\r\n");
        }

        private void setTextBoxText(TextBox view, String text) {
            view.Text = text;
        }

        public void appendLog(String line) {
            CavanDelegate.appendText(textBoxLog, line + "\r\n");
        }

        private bool readMacAddressFromFile(bool enable) {
            if (mMacAddress != null && mMacAddress.readFromFile()) {
                if (mMacAddress.AddressCount > 0) {
                    CavanDelegate.setEnable(buttonBurn, enable);
                } else {
                    CavanDelegate.setEnable(buttonBurn, false);
                }

                CavanDelegate.setText(textBoxMacAddressStart, mMacAddress.ToString());
                CavanDelegate.setText(textBoxMacAddressEnd, mMacAddress.AddressEnd.ToString());
                CavanDelegate.setText(textBoxMacAddressCount, mMacAddress.AddressCount + " (个)");

                return true;
            } else {
                CavanDelegate.setEnable(buttonBurn, false);

                CavanDelegate.clearText(textBoxMacAddressStart);
                CavanDelegate.clearText(textBoxMacAddressEnd);
                CavanDelegate.clearText(textBoxMacAddressCount);

                return false;
            }
        }

        private String findSmartSnippetsPath() {
            foreach (String path in sProgramFiles) {
                String rootDir = Path.Combine(path, "SmartSnippets");
                if (Directory.Exists(rootDir)) {
                    String binDir = Path.Combine(rootDir, "bin");
                    if (Directory.Exists(binDir)) {
                        String smartSnippetsPath = Path.Combine(binDir, "SmartSnippets.exe");
                        if (File.Exists(smartSnippetsPath)) {
                            return smartSnippetsPath;
                        }
                    }
                }
            }

            return null;
        }

        private String getSmartSnippetsPath() {
            if (mFileSmartSnippetsExe != null && File.Exists(mFileSmartSnippetsExe)) {
                return mFileSmartSnippetsExe;
            }

            mFileSmartSnippetsExe = findSmartSnippetsPath();
            if (mFileSmartSnippetsExe != null) {
                return mFileSmartSnippetsExe;
            }

            if (openFileDialogSmartSnippets.ShowDialog() != DialogResult.OK) {
                return null;
            }

            mFileSmartSnippetsExe = openFileDialogSmartSnippets.FileName;

            return mFileSmartSnippetsExe;
        }

        private String doRunCommand(ShellCommandRunner runner) {
            if (runner.execute()) {
                return runner.LastOutputLine;
            }

            return null;
        }

        private String runSmartSnippetsCommand(params String[] args) {
            ShellCommandRunner runner = new ShellCommandRunner(getSmartSnippetsPath(), this);
            runner.addArguments(args);
            return doRunCommand(runner);
        }

        private String runOtpCommand(bool withFirmware, params String[] args) {
            ShellCommandRunner runner = new ShellCommandRunner(getSmartSnippetsPath(), this);
            runner.addArguments(sOtpCommandArgs);

            if (withFirmware) {
                if (!File.Exists(sFileProgrammerBin)) {
                    MessageBox.Show("找不到文件：" + sFileProgrammerBin);
                    return null;
                }

                runner.addArguments("-firmware", sFileProgrammerBin);
            }

            runner.addArguments(args);

            return doRunCommand(runner);
        }

        private bool writeOtpData(String offset, String data) {
            // appendLog("写数据: " + data + " => " + offset);

            String line = runOtpCommand(false, "-cmd", "write_field", "-offset", offset, "-data", data);
            if (line == null) {
                return false;
            }

            if (line.StartsWith("Failed")) {
                return false;
            }

            return line.StartsWith("Burned");
        }

        private String getBytesHexString(byte[] bytes) {
            StringBuilder builder = new StringBuilder();

            foreach (byte value in bytes) {
                CavanString.fromByte(builder, value);
            }

            return builder.ToString();
        }

        private bool writeOtpData(String offset, byte[] data) {
            return writeOtpData(offset, getBytesHexString(data));
        }

        private bool writeBdAddress() {
            if (mMacAddress == null || mMacAddress.AddressCount <= 0) {
                return false;
            }

            appendLog("写MAC地址：" + mMacAddress);

            if (!writeOtpData("0x7FD4", mMacAddress.getBytes())) {
                return false;
            }

            textBoxMacAddressNow.Text = mMacAddress.ToString();

            if (mMacAddress.increaseAndSave()) {
                return true;
            }

            return false;
        }

        private bool setOtpBootEnable() {
            appendLog("设置从OTP启动");

            return writeOtpData("0x7F00", "1234A5A5A5A51234");
        }

        private bool readOtpHeader(String pathname) {
            appendLog("读取OTP头部到：" + pathname);

            String line = runOtpCommand(true, "-cmd", "read_header", "-file", pathname);
            if (line == null) {
                return false;
            }

            if (line.StartsWith("Failed")) {
                return false;
            }

            return line.StartsWith("Reading is complete");
        }

        private byte[] readOtpHeader() {
            if (!readOtpHeader(sFileOtpHeaderBin)) {
                return null;
            }

            return File.ReadAllBytes(sFileOtpHeaderBin);
        }

        private bool readOtpFirmware(String pathname) {
            appendLog("从OTP读取固件到：" + pathname);

            String line = runOtpCommand(true, "-cmd", "read_custom_code", "-file", pathname);
            if (line == null) {
                return false;
            }

            if (line.StartsWith("OTP memory reading has failed")) {
                return false;
            }

            return line.StartsWith("OTP memory reading has finished");
        }

        private byte[] readOtpFirmware() {
            if (!readOtpFirmware(sFileOtpFirmwareBin)) {
                return null;
            }

            return File.ReadAllBytes(sFileOtpFirmwareBin);
        }

        private bool writeOtpFirmware(String pathname) {
            appendLog("写固件文件到OTP：" + pathname);

            String line = runOtpCommand(false, "-cmd", "write_custom_code", "-y", "-file", pathname);
            if (line == null) {
                return false;
            }

            if (line.StartsWith("OTP Memory burning failed")) {
                return false;
            }

            return line.StartsWith("OTP Memory burning completed successfully");
        }

        private bool isMemoryEmpty(byte[] bytes, int offset, int length) {
            for (int end = offset + length; offset < end; offset++) {
                if (bytes[offset] != 0x00) {
                    return false;
                }
            }

            return true;
        }

        private bool isMemeoryMatch(byte[] mem1, int off1, byte[] mem2, int off2, int length) {
            for (int i = 0; i < length; i++) {
                if (mem1[off1 + i] != mem2[off2 + i]) {
                    return false;
                }
            }

            return true;
        }

        private bool burnOtpFirmwareAll(String pathname) {
            byte[] bytes = readOtpFirmware();
            if (bytes == null) {
                MessageBox.Show("读取固件失败！");
                return false;
            }

            appendLog("成功");

            CavanDelegate.setText(textBoxMacAddressNow, new JwaooMacAddress().fromOtpFirmware(bytes).ToString());

            if (isMemoryEmpty(bytes, 0, 0x7F00)) {
                if (!writeOtpFirmware(pathname)) {
                    MessageBox.Show("写固件失败: " + pathname);
                    return false;
                }

                appendLog("成功");
            } else {
                // MessageBox.Show("OTP中的固件不为空，可能已经写过了");
                appendLog("已经写过固件了，直接跳过");
            }

            if (isMemoryEmpty(bytes, 0x7FD4, 6)) {
                if (!writeBdAddress()) {
                    MessageBox.Show("写MAC地址失败！");
                    return false;
                }

                appendLog("成功");
            } else {
                // MessageBox.Show("OTP中的MAC地址不为空，可能已经写过了");
                appendLog("已经写过MAC地址了，直接跳过");
            }

            if (isMemoryEmpty(bytes, 0x7F00, 8)) {
                if (!setOtpBootEnable()) {
                    MessageBox.Show("设置从OTP启动失败！");
                    return false;
                }

                appendLog("成功");
            } else if (isMemeoryMatch(bytes, 0x7F00, sOtpBootMagic, 0, sOtpBootMagic.Length)) {
                appendLog("已经设置从OTP启动了，直接跳过");
            } else {
                MessageBox.Show("OTP启动标志位不匹配！！！");
                return false;
            }

            return true;
        }

        private void buttonFirmware_Click(object sender, EventArgs e) {
            if (openFileDialogFirmware.ShowDialog() == DialogResult.OK) {
                setFwFilePath(openFileDialogFirmware.FileName);
            }
        }

        // ================================================================================

        private void backgroundWorkerConnTest_ProgressChanged(object sender, System.ComponentModel.ProgressChangedEventArgs e) {
            switch (e.ProgressPercentage) {
            case 1:
                labelState.Text = "正在测试连接...";
                labelState.ForeColor = System.Drawing.Color.Black;
                break;

            case 2:
                appendLog("连接成功");
                labelState.Text = "连接成功";
                labelState.ForeColor = System.Drawing.Color.LimeGreen;

                textBoxMacAddressNow.Text = new JwaooMacAddress().fromOtpHeader((byte[])e.UserState).ToString();
                break;

            case 3:
                appendLog("连接失败！！！");
                labelState.Text = "连接失败！";
                labelState.ForeColor = System.Drawing.Color.Red;
                break;
            }
        }

        private void backgroundWorkerConnTest_RunWorkerCompleted(object sender, System.ComponentModel.RunWorkerCompletedEventArgs e) {
            buttonConnect.Enabled = true;
        }

        private void backgroundWorkerConnTest_DoWork(object sender, System.ComponentModel.DoWorkEventArgs e) {

            backgroundWorkerConnTest.ReportProgress(1);

            byte[] bytes = readOtpHeader();
            if (bytes != null) {
                backgroundWorkerConnTest.ReportProgress(2, bytes);
                readMacAddressFromFile(true);
            } else {
                backgroundWorkerConnTest.ReportProgress(3);
            }
        }

        private void buttonConnect_Click(object sender, EventArgs e) {
            buttonConnect.Enabled = false;
            buttonBurn.Enabled = false;
            backgroundWorkerConnTest.RunWorkerAsync();
        }

        // ================================================================================

        private void backgroundWorkerOtpBurn_ProgressChanged(object sender, System.ComponentModel.ProgressChangedEventArgs e) {
            switch (e.ProgressPercentage) {
            case 1:
                labelState.Text = "正在烧录...";
                labelState.ForeColor = System.Drawing.Color.Black;
                break;

            case 2:
                appendLog("烧录成功");
                labelState.Text = "烧录成功";
                labelState.ForeColor = System.Drawing.Color.LimeGreen;
                break;

            case 3:
                appendLog("烧录失败！！！");
                labelState.Text = "烧录失败！";
                labelState.ForeColor = System.Drawing.Color.Red;
                break;

            case 4:
                labelState.Text = "MAC地址用完了，请重新分配!";
                labelState.ForeColor = System.Drawing.Color.Red;
                break;
            }
        }

        private void backgroundWorkerOtpBurn_RunWorkerCompleted(object sender, System.ComponentModel.RunWorkerCompletedEventArgs e) {
            buttonConnect.Enabled = true;
            buttonFirmware.Enabled = true;
        }

        private void backgroundWorkerOtpBurn_DoWork(object sender, System.ComponentModel.DoWorkEventArgs e) {
            String pathname = (String)e.Argument;

            if (pathname.Length == 0) {
                MessageBox.Show("请选择固件文件");
            } else if (!File.Exists(pathname)) {
                MessageBox.Show("固件文件不存在：" + pathname);
            } else if (mMacAddress == null) {
                MessageBox.Show("请选择正确的固件文件：" + pathname);
            } else if (!readMacAddressFromFile(false)) {
                MessageBox.Show("读取MAC地址出错：" + mMacAddress.FilePath);
            } else if (mMacAddress.AddressCount > 0) {
                backgroundWorkerOtpBurn.ReportProgress(1);

                if (burnOtpFirmwareAll(pathname)) {
                    mBurnSuccess = true;
                    backgroundWorkerOtpBurn.ReportProgress(2);
                } else {
                    backgroundWorkerOtpBurn.ReportProgress(3);
                }

                readMacAddressFromFile(true);
            } else {
                backgroundWorkerOtpBurn.ReportProgress(4);
            }
        }

        private void buttonBurn_Click(object sender, EventArgs e) {
            buttonBurn.Enabled = false;
            buttonConnect.Enabled = false;
            buttonFirmware.Enabled = false;

            if (mBurnSuccess) {
                mBurnSuccess = false;
                textBoxLog.Clear();
            }

            backgroundWorkerOtpBurn.RunWorkerAsync(textBoxFirmware.Text);
        }

        // ================================================================================

        private void buttonClearLog_Click(object sender, EventArgs e) {
            if (mFileStreamLog != null) {
                mFileStreamLog.SetLength(0);
            }

            textBoxLog.Clear();
        }

        private void buttonMacAlloc_Click(object sender, EventArgs e) {
            CavanMacAddress address = new CavanMacAddress();
            UInt32 count;

            if (mMacAddress != null) {
                count = mMacAddress.AddressCount;
                address.copyFrom(mMacAddress);
            } else {
                count = 0;
            }

            CavanMacAddressManager manager = new CavanMacAddressManager(address, count);
            manager.Show(this);
        }

        private void buttonAddressEdit_Click(object sender, EventArgs e) {
            if (readMacAddressFromFile(true)) {
                JwaooMacAddressEditDialog dialog = new JwaooMacAddressEditDialog(mMacAddress);
                if (dialog.ShowDialog() == DialogResult.OK) {
                    mMacAddress.writeToFile();
                    readMacAddressFromFile(true);
                }
            } else {
                MessageBox.Show("请先选择正确的固件文件！");
            }
        }
    }

    public class ShellCommandRunner {

        private String mCommand;
        private JwaooOtpProgrammer mProgrammer;
        private List<String> mErrLines = new List<string>();
        private List<String> mOutLines = new List<string>();
        private List<String> mArgumants = new List<string>();

        public ShellCommandRunner(String command, JwaooOtpProgrammer programmer) {
            mCommand = command;
            mProgrammer = programmer;
        }

        public void setArguments(List<String> args) {
            if (args == null) {
                mArgumants.Clear();
            } else {
                mArgumants = args;
            }
        }

        public void setArguments(params String[] args) {
            mArgumants = new List<string>(args.Length);
            addArguments(args);
        }

        public void addArgument(String arg) {
            mArgumants.Add(arg);
        }

        public void addArguments(params String[] args) {
            foreach (String arg in args) {
                mArgumants.Add(arg);
            }
        }

        public void addArguments(List<String> args) {
            foreach (String arg in args) {
                mArgumants.Add(arg);
            }
        }

        private String linesToString(List<String> lines) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < lines.Count; i++) {
                builder.Append(i + ". " + lines[i] + "\r\n");
            }

            return builder.ToString();
        }

        public bool execute() {
            mProgrammer.writeLog("================================================================================");
            mProgrammer.writeLog("command = " + mCommand);

            if (mCommand == null) {
                return false;
            }

            Process process = new Process();

            if (mArgumants != null && mArgumants.Count > 0) {
                bool needSpace = false;
                StringBuilder builder = new StringBuilder();

                foreach (String arg in mArgumants) {
                    if (needSpace) {
                        builder.Append(" \"");
                    } else {
                        needSpace = true;
                        builder.Append('"');
                    }

                    builder.Append(arg);
                    builder.Append('"');
                }

                process.StartInfo.Arguments = builder.ToString();
            }

            process.StartInfo.FileName = mCommand;
            process.StartInfo.UseShellExecute = false;
            process.StartInfo.CreateNoWindow = true;
            process.StartInfo.RedirectStandardOutput = true;
            process.StartInfo.RedirectStandardError = true;
            process.OutputDataReceived += Process_OutputDataReceived;
            process.ErrorDataReceived += Process_ErrorDataReceived;

            mProgrammer.writeLog("arguments = " + process.StartInfo.Arguments);

            mOutLines.Clear();
            mErrLines.Clear();

            try {
                if (process.Start()) {
                    process.BeginOutputReadLine();
                    process.BeginErrorReadLine();
                    process.WaitForExit();

                    if (process.ExitCode != 0) {
                        return false;
                    }

                    return true;
                }
            } catch (Exception e) {
                MessageBox.Show("运行命令出错: " + e);
            }

            return false;
        }

        public List<String> OutputLines {
            get {
                return mOutLines;
            }
        }

        public int OutputLineCount {
            get {
                return mOutLines.Count;
            }
        }

        public String LastOutputLine {
            get {
                int count = mOutLines.Count;
                if (count > 0) {
                    return mOutLines[count - 1];
                }

                return null;
            }
        }

        public List<String> ErrorLines {
            get {
                return mErrLines;
            }
        }

        public String getOutputLine(int index) {
            return mOutLines[index];
        }

        public String getErrorLine(int index) {
            return mErrLines[index];
        }

        private void Process_ErrorDataReceived(object sender, DataReceivedEventArgs e) {
            if (e.Data != null) {
                mErrLines.Add(e.Data);
                mProgrammer.writeLog(e.Data);
            }
        }

        private void Process_OutputDataReceived(object sender, DataReceivedEventArgs e) {
            if (e.Data != null) {
                mOutLines.Add(e.Data);
                mProgrammer.writeLog(e.Data);
            }
        }
    }
}
