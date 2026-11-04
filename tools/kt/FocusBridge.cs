using System;
using System.Drawing;
using System.Windows.Forms;

// Собственное нейтральное окно, без GLFW и без управления другими приложениями.
internal static class FocusBridge
{
    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        var form = new Form {
            Text = "KT native focus bridge",
            ClientSize = new Size(420, 160),
            StartPosition = FormStartPosition.CenterScreen
        };
        form.Controls.Add(new Label {
            Text = "Thaumcraft KT: neutral focus window",
            Dock = DockStyle.Fill,
            TextAlign = ContentAlignment.MiddleCenter
        });
        Application.Run(form);
    }
}
