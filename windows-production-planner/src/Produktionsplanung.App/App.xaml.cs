using System.Windows;
using Microsoft.EntityFrameworkCore;
using Produktionsplanung.App.Data;

namespace Produktionsplanung.App;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        using var db = new AppDbContext();
        db.Database.EnsureCreated();
        DemoDataSeeder.Seed(db);
    }
}
