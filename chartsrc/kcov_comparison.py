import matplotlib.pyplot as plt
from math import ceil, sqrt

class KcovChartBuilder:
    def __init__(self, charts_dir, dataKcovsMean, dataKcovsStd, algos, labels):
        self.charts_dir = charts_dir
        self.dataKcovsMean = dataKcovsMean
        self.dataKcovsStd = dataKcovsStd
        self.algos = algos
        self.labels = labels
        


    def compare(self, columns, fixesData, variableData, fancyTitle='', precision=0):
        if(fancyTitle == ''):
            fancyTitle = columns[2]
        l = self.labels

        for r,fixedD in enumerate(fixesData):
            fig = plt.figure(figsize=(14,10))
            for j,variableD in enumerate(variableData):
                # rows, columns, index
                size = ceil(sqrt(len(variableData)))
                rows = size
                cols = size
                ax = fig.add_subplot(rows, cols,j+1)
                ax.set_ylim([0,1])
                ax.set_title(fancyTitle + " = {0:.{1}f}".format(variableD, precision))
                if j%cols == 0:
                    ax.set_ylabel("Coverage (%)")
                plt.xticks(rotation=35, ha='right')
                ax.yaxis.grid(True)

                for i,s in enumerate(l.variables):
                    kwargs_arr = [dict(zip(columns, (algoname, fixedD, variableD))) for algoname in self.algos]
                    values = [self.dataKcovsMean[s].sel(**kwargs).values.tolist() for kwargs in kwargs_arr]
                    errors = [self.dataKcovsStd[s].sel(**kwargs).values.tolist() for kwargs in kwargs_arr]
                    ax.bar(self.algos, values, yerr=errors, label=l.legends[i], capsize=4, color=l.colors[i], ecolor=l.errColors[i])
                if j == cols-1:
                    ax.legend()
            plt.tight_layout()
            fig.savefig(self.charts_dir + 'KCov_compare_' + str(columns[1]) + '-' + str(fixedD) + '_' + str(columns[2]) + '_variable.pdf')
            plt.close(fig)


    def lines(self, columns, fixesData, variableData, variableColumName, fancyLabel, precision=0):
        l = self.labels
        for fixedD in fixesData:
            fig = plt.figure(figsize=(14,10))
            for idx,algo in enumerate(self.algos):
                cols = 2
                rows = ceil(len(self.algos) /cols)
                ax = fig.add_subplot(rows,cols,idx+1)
                minRange = min(variableData) - 0.1
                maxRange = max(variableData) + 0.1
                ax.set_ylim([0,1])
                ax.set_xlim([minRange, maxRange])
                plt.xticks(rotation=35, ha='right')
                if idx%cols == 0:
                    ax.set_ylabel("Coverage (%)")
                if idx >= len(self.algos) - rows:
                    ax.set_xlabel(fancyLabel)
                if idx%rows != 0:
                    ax.set_yticklabels([])
                ax.set_title(algo)
                ax.set_xticks([minRange] + variableData + [maxRange])
                ax.set_xticklabels([""] + ["{0:.{1}f}".format(c, precision) for c in variableData] + [""])

                kwargs = dict(zip(columns, (algo, fixedD)))

                chartdataMean = self.dataKcovsMean.sel(**kwargs)
                chartdataStd = self.dataKcovsStd.sel(**kwargs)
                for i,s in enumerate(l.variables):
                    values = chartdataMean[s].values.tolist()
                    errors = chartdataStd[s].values.tolist()
                    ax.plot(variableData, values, label=l.legends[i], color=l.colors[i])
                    for j,r in enumerate(variableData):
                        ax.errorbar(r, values[j], yerr=errors[j], fmt='', color=l.colors[i], elinewidth=1, capsize=0)
                if idx == cols-1:
                    ax.legend()
            plt.tight_layout()
            fig.savefig(self.charts_dir + 'KCov_lines_' + variableColumName + '-variable_'+ str(columns[1]) + "-" +str(fixedD)+'.pdf')
            plt.close(fig)


    def inTime(self, columns, kcovTypes, dataInTime, fixedType, variableTypes, timeLimit, name = ''):
        for whichKCov in kcovTypes:
            rows = 2
            cols = 2
            fig, axes = plt.subplots(rows, cols, figsize=(8,5), sharex='col', sharey='row')
            for idx, variableType in enumerate(variableTypes):
                r = int(idx / cols)
                c = int(idx % cols)

                kwargs = dict(zip(columns, (self.algos, fixedType, variableType)))

                xdata = dataInTime.sel(**kwargs)['time']
                ydata = dataInTime.sel(**kwargs)[whichKCov].transpose()
                timeLimitIdx = next((i for i,x in enumerate(xdata) if x >= timeLimit)) # first idx of time > timeLimit
                xdata = xdata[:timeLimitIdx]
                ydata = ydata[:timeLimitIdx]

                axes[r][c].plot(xdata, ydata)
                axes[r][c].set_title('n/m = ' + variableType)
                axes[r][c].set_ylim([0,1])
                if c == 0:
                    axes[r][c].set_ylabel(whichKCov + ' (%)')
                if r == rows-1:
                    axes[r][c].set_xlabel('t')
                if r == 0 and c == cols -1:
                    axes[r][c].legend(ydata.coords['Algorithm'].data.tolist())
            fig.savefig(self.charts_dir + whichKCov + "_" + name + '_InTime.pdf')
            plt.close(fig)
    
    def inTimeByValue(self, columns, kcovTypes, dataInTime, fixedType, variableTypes, timeLimit, name = ''):
        for whichKCov in kcovTypes:
            rows = 2
            cols = 2
            fig, axes = plt.subplots(rows, cols, figsize=(8,5), sharex='col', sharey='row')
            for idx, algorithm in enumerate(self.algos):
                r = int(idx / cols)
                c = int(idx % cols)

                kwargs = dict(zip(columns, (algorithm, fixedType, variableTypes)))

                xdata = dataInTime.sel(**kwargs)['time']
                ydata = dataInTime.sel(**kwargs)[whichKCov].transpose()
                timeLimitIdx = next((i for i,x in enumerate(xdata) if x >= timeLimit)) # first idx of time > timeLimit
                xdata = xdata[:timeLimitIdx]
                ydata = ydata[:timeLimitIdx]

                axes[r][c].plot(xdata, ydata)
                axes[r][c].set_title('n/m = ' + algorithm)
                axes[r][c].set_ylim([0,1])
                if c == 0:
                    axes[r][c].set_ylabel(whichKCov + ' (%)')
                if r == rows-1:
                    axes[r][c].set_xlabel('t')
                if r == 0 and c == cols -1:
                    axes[r][c].legend(ydata.coords[columns[2]].data.tolist())
            fig.savefig(self.charts_dir + whichKCov + "_" + name + '_InTime.pdf')
            plt.close(fig)