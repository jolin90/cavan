﻿namespace JwaooOtpProgrammer {
    partial class JwaooOtpProgrammer {
        /// <summary>
        /// 必需的设计器变量。
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// 清理所有正在使用的资源。
        /// </summary>
        /// <param name="disposing">如果应释放托管资源，为 true；否则为 false。</param>
        protected override void Dispose(bool disposing) {
            if (disposing && (components != null)) {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows 窗体设计器生成的代码

        /// <summary>
        /// 设计器支持所需的方法 - 不要修改
        /// 使用代码编辑器修改此方法的内容。
        /// </summary>
        private void InitializeComponent() {
            this.label1 = new System.Windows.Forms.Label();
            this.textBoxFirmware = new System.Windows.Forms.TextBox();
            this.buttonFirmware = new System.Windows.Forms.Button();
            this.labelMacAddressStart = new System.Windows.Forms.Label();
            this.buttonConnect = new System.Windows.Forms.Button();
            this.buttonBurn = new System.Windows.Forms.Button();
            this.textBoxMacAddressStart = new System.Windows.Forms.TextBox();
            this.openFileDialogFirmware = new System.Windows.Forms.OpenFileDialog();
            this.openFileDialogSmartSnippets = new System.Windows.Forms.OpenFileDialog();
            this.textBoxLog = new System.Windows.Forms.TextBox();
            this.buttonClearLog = new System.Windows.Forms.Button();
            this.labelMacAddressNow = new System.Windows.Forms.Label();
            this.textBoxMacAddressNow = new System.Windows.Forms.TextBox();
            this.labelState = new System.Windows.Forms.Label();
            this.labelMacAddressCount = new System.Windows.Forms.Label();
            this.textBoxMacAddressCount = new System.Windows.Forms.TextBox();
            this.checkBoxSaveLog = new System.Windows.Forms.CheckBox();
            this.buttonMacAlloc = new System.Windows.Forms.Button();
            this.labelMacAddressEnd = new System.Windows.Forms.Label();
            this.textBoxMacAddressEnd = new System.Windows.Forms.TextBox();
            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(5, 277);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(65, 12);
            this.label1.TabIndex = 0;
            this.label1.Text = "固件文件：";
            // 
            // textBoxFirmware
            // 
            this.textBoxFirmware.Location = new System.Drawing.Point(75, 275);
            this.textBoxFirmware.Name = "textBoxFirmware";
            this.textBoxFirmware.ReadOnly = true;
            this.textBoxFirmware.Size = new System.Drawing.Size(429, 21);
            this.textBoxFirmware.TabIndex = 1;
            // 
            // buttonFirmware
            // 
            this.buttonFirmware.Font = new System.Drawing.Font("宋体", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.buttonFirmware.Location = new System.Drawing.Point(510, 272);
            this.buttonFirmware.Name = "buttonFirmware";
            this.buttonFirmware.Size = new System.Drawing.Size(35, 23);
            this.buttonFirmware.TabIndex = 2;
            this.buttonFirmware.Text = "...";
            this.buttonFirmware.UseVisualStyleBackColor = true;
            this.buttonFirmware.Click += new System.EventHandler(this.buttonFirmware_Click);
            // 
            // labelMacAddressStart
            // 
            this.labelMacAddressStart.AutoSize = true;
            this.labelMacAddressStart.Location = new System.Drawing.Point(5, 308);
            this.labelMacAddressStart.Name = "labelMacAddressStart";
            this.labelMacAddressStart.Size = new System.Drawing.Size(83, 12);
            this.labelMacAddressStart.TabIndex = 3;
            this.labelMacAddressStart.Text = "起始MAC地址：";
            // 
            // buttonConnect
            // 
            this.buttonConnect.Location = new System.Drawing.Point(277, 366);
            this.buttonConnect.Name = "buttonConnect";
            this.buttonConnect.Size = new System.Drawing.Size(134, 77);
            this.buttonConnect.TabIndex = 5;
            this.buttonConnect.Text = "连接测试";
            this.buttonConnect.UseVisualStyleBackColor = true;
            this.buttonConnect.Click += new System.EventHandler(this.buttonConnect_Click);
            // 
            // buttonBurn
            // 
            this.buttonBurn.Enabled = false;
            this.buttonBurn.Location = new System.Drawing.Point(413, 366);
            this.buttonBurn.Name = "buttonBurn";
            this.buttonBurn.Size = new System.Drawing.Size(132, 77);
            this.buttonBurn.TabIndex = 6;
            this.buttonBurn.Text = "烧录";
            this.buttonBurn.UseVisualStyleBackColor = true;
            this.buttonBurn.Click += new System.EventHandler(this.buttonBurn_Click);
            // 
            // textBoxMacAddressStart
            // 
            this.textBoxMacAddressStart.Location = new System.Drawing.Point(94, 304);
            this.textBoxMacAddressStart.Name = "textBoxMacAddressStart";
            this.textBoxMacAddressStart.ReadOnly = true;
            this.textBoxMacAddressStart.Size = new System.Drawing.Size(151, 21);
            this.textBoxMacAddressStart.TabIndex = 4;
            // 
            // openFileDialogFirmware
            // 
            this.openFileDialogFirmware.Filter = "固件文件|JwaooFwModel*.hex";
            this.openFileDialogFirmware.Title = "选择固件文件";
            // 
            // openFileDialogSmartSnippets
            // 
            this.openFileDialogSmartSnippets.FileName = "SmartSnippets.exe";
            this.openFileDialogSmartSnippets.Filter = "SmartSnippets|SmartSnippets.exe";
            this.openFileDialogSmartSnippets.Title = "选择文件 SmartSnippets.exe";
            // 
            // textBoxLog
            // 
            this.textBoxLog.Location = new System.Drawing.Point(5, 4);
            this.textBoxLog.Multiline = true;
            this.textBoxLog.Name = "textBoxLog";
            this.textBoxLog.ReadOnly = true;
            this.textBoxLog.ScrollBars = System.Windows.Forms.ScrollBars.Both;
            this.textBoxLog.Size = new System.Drawing.Size(540, 231);
            this.textBoxLog.TabIndex = 7;
            this.textBoxLog.WordWrap = false;
            // 
            // buttonClearLog
            // 
            this.buttonClearLog.Location = new System.Drawing.Point(141, 366);
            this.buttonClearLog.Name = "buttonClearLog";
            this.buttonClearLog.Size = new System.Drawing.Size(134, 77);
            this.buttonClearLog.TabIndex = 8;
            this.buttonClearLog.Text = "清空日志";
            this.buttonClearLog.UseVisualStyleBackColor = true;
            this.buttonClearLog.Click += new System.EventHandler(this.buttonClearLog_Click);
            // 
            // labelMacAddressNow
            // 
            this.labelMacAddressNow.AutoSize = true;
            this.labelMacAddressNow.Location = new System.Drawing.Point(305, 336);
            this.labelMacAddressNow.Name = "labelMacAddressNow";
            this.labelMacAddressNow.Size = new System.Drawing.Size(83, 12);
            this.labelMacAddressNow.TabIndex = 9;
            this.labelMacAddressNow.Text = "当前MAC地址：";
            // 
            // textBoxMacAddressNow
            // 
            this.textBoxMacAddressNow.Location = new System.Drawing.Point(394, 333);
            this.textBoxMacAddressNow.Name = "textBoxMacAddressNow";
            this.textBoxMacAddressNow.ReadOnly = true;
            this.textBoxMacAddressNow.Size = new System.Drawing.Size(151, 21);
            this.textBoxMacAddressNow.TabIndex = 10;
            // 
            // labelState
            // 
            this.labelState.AutoSize = true;
            this.labelState.Font = new System.Drawing.Font("宋体", 18F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.labelState.Location = new System.Drawing.Point(5, 243);
            this.labelState.Name = "labelState";
            this.labelState.Size = new System.Drawing.Size(60, 24);
            this.labelState.TabIndex = 11;
            this.labelState.Text = "状态";
            // 
            // labelMacAddressCount
            // 
            this.labelMacAddressCount.AutoSize = true;
            this.labelMacAddressCount.Location = new System.Drawing.Point(5, 337);
            this.labelMacAddressCount.Name = "labelMacAddressCount";
            this.labelMacAddressCount.Size = new System.Drawing.Size(83, 12);
            this.labelMacAddressCount.TabIndex = 12;
            this.labelMacAddressCount.Text = "剩余MAC地址：";
            // 
            // textBoxMacAddressCount
            // 
            this.textBoxMacAddressCount.Location = new System.Drawing.Point(94, 333);
            this.textBoxMacAddressCount.Name = "textBoxMacAddressCount";
            this.textBoxMacAddressCount.ReadOnly = true;
            this.textBoxMacAddressCount.Size = new System.Drawing.Size(151, 21);
            this.textBoxMacAddressCount.TabIndex = 13;
            // 
            // checkBoxSaveLog
            // 
            this.checkBoxSaveLog.AutoSize = true;
            this.checkBoxSaveLog.Location = new System.Drawing.Point(437, 242);
            this.checkBoxSaveLog.Name = "checkBoxSaveLog";
            this.checkBoxSaveLog.Size = new System.Drawing.Size(108, 16);
            this.checkBoxSaveLog.TabIndex = 14;
            this.checkBoxSaveLog.Text = "保存日志到文件";
            this.checkBoxSaveLog.UseVisualStyleBackColor = true;
            // 
            // buttonMacAlloc
            // 
            this.buttonMacAlloc.Location = new System.Drawing.Point(5, 366);
            this.buttonMacAlloc.Name = "buttonMacAlloc";
            this.buttonMacAlloc.Size = new System.Drawing.Size(134, 77);
            this.buttonMacAlloc.TabIndex = 15;
            this.buttonMacAlloc.Text = "地址分配";
            this.buttonMacAlloc.UseVisualStyleBackColor = true;
            this.buttonMacAlloc.Click += new System.EventHandler(this.buttonMacAlloc_Click);
            // 
            // labelMacAddressEnd
            // 
            this.labelMacAddressEnd.AutoSize = true;
            this.labelMacAddressEnd.Location = new System.Drawing.Point(305, 307);
            this.labelMacAddressEnd.Name = "labelMacAddressEnd";
            this.labelMacAddressEnd.Size = new System.Drawing.Size(83, 12);
            this.labelMacAddressEnd.TabIndex = 16;
            this.labelMacAddressEnd.Text = "结束MAC地址：";
            // 
            // textBoxMacAddressEnd
            // 
            this.textBoxMacAddressEnd.Location = new System.Drawing.Point(394, 304);
            this.textBoxMacAddressEnd.Name = "textBoxMacAddressEnd";
            this.textBoxMacAddressEnd.ReadOnly = true;
            this.textBoxMacAddressEnd.Size = new System.Drawing.Size(151, 21);
            this.textBoxMacAddressEnd.TabIndex = 17;
            // 
            // JwaooOtpProgrammer
            // 
            this.AcceptButton = this.buttonBurn;
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 12F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.AutoScroll = true;
            this.AutoSize = true;
            this.ClientSize = new System.Drawing.Size(550, 447);
            this.Controls.Add(this.textBoxMacAddressEnd);
            this.Controls.Add(this.labelMacAddressEnd);
            this.Controls.Add(this.buttonMacAlloc);
            this.Controls.Add(this.checkBoxSaveLog);
            this.Controls.Add(this.textBoxMacAddressCount);
            this.Controls.Add(this.labelMacAddressCount);
            this.Controls.Add(this.labelState);
            this.Controls.Add(this.textBoxMacAddressNow);
            this.Controls.Add(this.labelMacAddressNow);
            this.Controls.Add(this.buttonClearLog);
            this.Controls.Add(this.textBoxLog);
            this.Controls.Add(this.buttonBurn);
            this.Controls.Add(this.buttonConnect);
            this.Controls.Add(this.textBoxMacAddressStart);
            this.Controls.Add(this.labelMacAddressStart);
            this.Controls.Add(this.buttonFirmware);
            this.Controls.Add(this.textBoxFirmware);
            this.Controls.Add(this.label1);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.Name = "JwaooOtpProgrammer";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "井蛙OTP烧录器";
            this.FormClosed += new System.Windows.Forms.FormClosedEventHandler(this.Programmer_FormClosed);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.TextBox textBoxFirmware;
        private System.Windows.Forms.Button buttonFirmware;
        private System.Windows.Forms.Label labelMacAddressStart;
        private System.Windows.Forms.Button buttonConnect;
        private System.Windows.Forms.Button buttonBurn;
        private System.Windows.Forms.TextBox textBoxMacAddressStart;
        private System.Windows.Forms.OpenFileDialog openFileDialogFirmware;
        private System.Windows.Forms.OpenFileDialog openFileDialogSmartSnippets;
        private System.Windows.Forms.TextBox textBoxLog;
        private System.Windows.Forms.Button buttonClearLog;
        private System.Windows.Forms.Label labelMacAddressNow;
        private System.Windows.Forms.TextBox textBoxMacAddressNow;
        private System.Windows.Forms.Label labelState;
        private System.Windows.Forms.Label labelMacAddressCount;
        private System.Windows.Forms.TextBox textBoxMacAddressCount;
        private System.Windows.Forms.CheckBox checkBoxSaveLog;
        private System.Windows.Forms.Button buttonMacAlloc;
        private System.Windows.Forms.Label labelMacAddressEnd;
        private System.Windows.Forms.TextBox textBoxMacAddressEnd;
    }
}
