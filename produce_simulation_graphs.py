# %%
import numpy as np
import xarray as xr

from math import ceil, sqrt

# Prepare the charting system
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.cm as cmx
from mpl_toolkits.mplot3d import Axes3D # needed for 3d projection
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

import chartsrc.kcov_comparison as kcovlib
from chartsrc.utils import *

# %%
if __name__ == '__main__':
    generateAll = True
    dataIncludeClusteringDistance = True

    main_experiment = "experiment_export"
    # CONFIGURE SCRIPT
    dir = "export-big-clustering"
    directory = 'app/build/' + dir + '/'
    charts_dir = 'app/build/charts-clustering/'
    pickleOutput = 'data_summary'
    experiments = [main_experiment]
    floatPrecision = '{: 0.2f}'
    seedVars = ['Seed']
    timeSamples = 360
    minTime = 0
    maxTime = 1800
    timeColumnName = 'time'
    logarithmicTime = False

    
    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os
    newestFileTime = max(os.path.getmtime(directory + '/' + file) for file in os.listdir(directory))
    try:
        lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
    except:
        lastTimeProcessed = -1
    shouldRecompute = newestFileTime != lastTimeProcessed
    datasets = dict()
    if not shouldRecompute:
        try:
            #means = pickle.load(open(pickleOutput + '_mean', 'rb'))
            #stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            datasets = pickle.load(open(pickleOutput + '_datasets', 'rb'))
        except:
            shouldRecompute = True
            
    if shouldRecompute:
        timefun = np.logspace if logarithmicTime else np.linspace
        means = {}
        stdevs = {}
        for experiment in experiments:
            # Collect all files for the experiment of interest
            import fnmatch
            allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '*.csv'), os.listdir(directory))
            allfiles = [directory + '/' + name for name in allfiles]
            allfiles.sort()
            # From the file name, extract the independent variables
            dimensions = {}
            print("Number of files:", len(allfiles))
            for file in allfiles:
                dimensions = mergeDicts(dimensions, extractCoordinates(file))
            dimensions = {k: sorted(v) for k, v in dimensions.items()}
            # Add time to the independent variables
            dimensions[timeColumnName] = range(0, timeSamples)
            # Compute the matrix shape
            shape = tuple(len(v) for k, v in dimensions.items())
            # Prepare the Dataset
            dataset = xr.Dataset()
            for k, v in dimensions.items():
                dataset.coords[k] = v
            varNames = extractVariableNames(allfiles[0])
            for v in varNames:
                if v != timeColumnName:
                    novals = np.ndarray(shape)
                    novals.fill(float('nan'))
                    dataset[v] = (dimensions.keys(), novals)
            # Compute maximum and minimum time, create the resample
            timeColumn = varNames.index(timeColumnName)
            allData = { file: np.matrix(openCsv(file)) for file in allfiles }
            computeMin = minTime is None
            computeMax = maxTime is None
            if computeMax:
                maxTime = float('-inf')
                for data in allData.values():
                    maxTime = max(maxTime, data[-1, timeColumn])
            if computeMin:
                minTime = float('inf')
                for data in allData.values():
                    minTime = min(minTime, data[0, timeColumn])
            #print(allData)
            timeline = timefun(minTime, maxTime, timeSamples)
            # Resample
            for file in allData:
                allData[file] = convert(timeColumn, timeline, allData[file])
                
            # Populate the dataset
            for file, data in allData.items():
                dataset[timeColumnName] = timeline
                for idx, v in enumerate(varNames):
                    if v != timeColumnName:
                        darray = dataset[v]
                        experimentVars = extractCoordinates(file)
                        darray.loc[experimentVars] = data[:, idx].A1
            #print(dataset)
            # Fold the dataset along the seed variables, producing the mean and stdev datasets
            #means[experiment] = dataset.mean(seedVars)
            #stdevs[experiment] = dataset.std(seedVars)
            datasets[experiment] = dataset
        # Save the datasets
        #pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
        #pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
        pickle.dump(datasets, open(pickleOutput + '_datasets', 'wb'), protocol=-1)
        pickle.dump(newestFileTime, open('timeprocessed', 'wb'))


    figure_size=(6, 6)
    matplotlib.rcParams.update({'axes.titlesize': 13})
    matplotlib.rcParams.update({'axes.labelsize': 12})
    
    kcovColors =  ['#00d0ebFF','#61a72cFF'] # ['#00d0ebFF','#61a72cFF','#e30000FF']
    kcovEcolors =  ['#0300ebFF', '#8cff9dFF'] # ['#0300ebFF', '#8cff9dFF', '#f5b342FF'] # error bars
    kcovVariables = ['1-coverage','2-coverage']
    kcovTrans = ['1-cov','2-cov']

    kcovLabels = Labels(kcovColors, kcovEcolors, kcovVariables, kcovTrans)

    data = datasets[main_experiment]
    # algos = ["ff_linpro_c", "ff_linpro", "sm_av_c", "sm_av", "bc_re_c", "bc_re", "ff_nocomm_c", "ff_nocomm"]
    algos = ["ff_linpro_c", "ff_linpro_ac"]
    algos = ["ff_linpro_c", "ff_linproF_c", "sm_av_c", "bc_re_c"]

#     algos = data.coords['Algorithm'].data.tolist()

    # now load data from previous simulations
    #print("loading old data...")
    #oldData = pickle.load(open('data_summary_datasets_20200106', 'rb'))['simulations']
    #print("merging data...")
    #data = xr.combine_by_coords([data, oldData])
    #print("generating charts...")
    #mergedDatasets = {'simulations': data}
    #pickle.dump(mergedDatasets, open(pickleOutput + '_datasets_merged', 'wb'), protocol=-1)
    
    dataMean = data.mean('time')
    dataKcovsMean = dataMean.mean('Seed').mean('ClusteringDistance') 
    dataKcovsStd = dataMean.std('Seed').mean('ClusteringDistance')
    
    dataDist = data.sum('time').assign(MovEfficiency = lambda d: d.ObjDist / d.CamDist)
    dataDistMean = dataDist.mean('Seed')
    dataDistStd = dataDist.std('Seed')
    
    simRatios = data.coords['CamHerdRatio'].data.tolist()
    simRatios.reverse()
    herdNumbers = data.coords['NumberOfHerds'].data.tolist()
    herdNumbers.reverse()


    kcovChartBuilder = kcovlib.KcovChartBuilder(charts_dir, dataKcovsMean, dataKcovsStd, algos, kcovLabels)

    
    def noOdds(lst): # replaces odds numbers in lst with empty strings
        return list(map(lambda x: x if round(x * 10, 0) % 2 == 0 else '', lst))
    
    """""""""""""""""""""""""""
                kcov 3D
    """""""""""""""""""""""""""
    #oldParams = matplotlib.rcParams.copy()
    #labelsize = 22
    #titlesize = 25
    #matplotlib.rcParams.update({'axes.titlesize': titlesize})
    #matplotlib.rcParams.update({'axes.labelsize': labelsize})
    def getSurfData(dataarray, xcord, ycord):
        xs = []
        ys = []
        zs = []
        for xd in dataarray:
            for i, yd in enumerate(xd):
                vals = xd[xcord].values
                if(isinstance(vals, np.ndarray) and vals.size > 1):
                    xs.append(vals.tolist()[i])
                else:
                    xs.append(vals.tolist())

                ys.append(yd[ycord].values.tolist())
                zs.append(yd.values.tolist())

        return xs, ys, zs
        
    fig = plt.figure(figsize=(12,16))
    for idx, algo in enumerate(algos):
        cols = 2
        rows = ceil(len(algos) / 2)
        ax = fig.add_subplot(rows,cols,idx+1, projection='3d')

        #ax.tick_params(labelsize=labelsize)
        ax.set_xlabel("m")
        ax.set_ylabel("n/m")
        if idx%cols == cols-1:
            ax.set_zlabel("Coverage (%)")
        #else:
        #    ax.set_zticklabels([])
        ax.set_xlim([max(herdNumbers),min(herdNumbers)])
        ax.set_ylim([min(simRatios),max(simRatios)])
        ax.set_zlim([0,1])
        ax.set_title(algo)
        
        fakeLinesForLegend = []
        def kcov(whichKCov,x,y):
            return dataKcovsMean[whichKCov].sel(Algorithm=algo, CamHerdRatio=y, NumberOfHerds=x).values.tolist()#[0]
        forKcovVars = [kcovVariables[0], kcovVariables[-1]]
        forKcovTrans = []
        for k, whichKCov in enumerate(kcovVariables):
            if not whichKCov in forKcovVars:
                continue
            x,y,z = getSurfData(dataKcovsMean[whichKCov].sel(Algorithm=algo), 'NumberOfHerds', 'CamHerdRatio')
            # print(x)
            # print(y)
            # print(z)
            ax.plot_trisurf(x,y,z, linewidth=2, antialiased=False, shade=True, alpha=0.5, color=kcovColors[k])
            fakeLinesForLegend.append(matplotlib.lines.Line2D([0],[0], linestyle='none', c=kcovColors[k], marker='o'))
            forKcovTrans.append(kcovTrans[k])
        if idx == cols-1:
            ax.legend(fakeLinesForLegend, forKcovTrans, numpoints=1)
        
    plt.tight_layout()
    fig.savefig(charts_dir + 'KCov_3D.pdf')
    plt.close(fig)
    #matplotlib.rcParams.update(oldParams)
    
    """""""""""""""""""""""""""
          kcov in time
    """""""""""""""""""""""""""
    timeLimit = timeSamples
    selAlgos = algos
    selRatios = [ '0.5', '1.0', '1.5', '2.0']
    selKcov = ['1-coverage', "2-coverage"]
    selHerdNumber = 6.0
    dataInTime = data.mean('Seed').mean("ClusteringDistance")


    columns = ['Algorithm', "NumberOfHerds", "CamHerdRatio"]
    kcovChartBuilder.inTime(columns, selKcov, dataInTime, selHerdNumber, selRatios, timeLimit)


    if(generateAll and dataIncludeClusteringDistance): # Only for clustering distance
        columns = ['Algorithm', "NumberOfHerds", "ClusteringDistance"]
        # selCamHerdRatio = 1.0 
        selHerdNumbers = [2.0, 4.0, 6.0, 8.0]
        dataInTime =  data.mean('Seed').mean("CamHerdRatio")
        distances = ['10', '30', '50', '70', '90']

        for selHerdNumber in selHerdNumbers:
            kcovChartBuilder.inTimeByValue(columns, selKcov, dataInTime, selHerdNumber, distances, timeLimit, name="clust-distances")
        
    """""""""""""""""""""""""""
              heatmaps
    """""""""""""""""""""""""""
    simRatios.reverse()
    herdNumbers.reverse()
    import seaborn as sns
    rows = 4
    cols = 2
    gridspec_kw={'width_ratios': [1,1,0.05], 'height_ratios': [1,1,1,1]}
    for whichKCov in kcovVariables:
        fig, axes = plt.subplots(rows, cols+1, figsize=(8,10), sharex='col', gridspec_kw=gridspec_kw)
        plt.xlim([min(simRatios), max(simRatios)])
        plt.ylim([0,1])
        for idx,algo in enumerate(algos):
            r = int(idx / cols)
            c = int(idx % cols)
            data = dataKcovsMean.sel(Algorithm=algo)[whichKCov]
            cbar = idx%cols == cols - 1 # only charts to the right have the bar
            ax = sns.heatmap(data, vmin=0, vmax=1, ax=axes[r][c], cbar=cbar, cbar_ax=axes[r][cols], cbar_kws={'label': whichKCov + ' (%)'})
            if idx%cols == 0:
                ax.set_ylabel('r')
                ax.set_yticklabels([str(int(x)) for x in herdNumbers])
            else:
                ax.set_yticklabels([])
            if idx >= cols * (rows - 1):
                ax.set_xlabel('n/m')
                ax.set_xticklabels(noOdds(simRatios))
                
            ax.invert_yaxis()
            ax.set_title(algo)
        fig.savefig(charts_dir + whichKCov + '_heatmap.pdf')
        plt.close(fig)
    simRatios.reverse()
    herdNumbers.reverse()
    
    """""""""""""""""""""""""""
           kcov lines
    """""""""""""""""""""""""""
    simRatios.reverse()
    herdNumbers.reverse()


    if(generateAll and False):
        cols = ["Algorithm", 'NumberOfHerds']
        kcovChartBuilder.lines(cols, herdNumbers, simRatios, "CamHerdRatio", "n/m", precision=1)
            
        cols = ["Algorithm", 'CamHerdRatio']
        kcovChartBuilder.lines(cols, simRatios, herdNumbers, "herdNumber", "Number of herds")
        
    simRatios.reverse()
    herdNumbers.reverse()  


    """""""""""""""""""""""""""
        kcoverage comparison
    """""""""""""""""""""""""""
    if(generateAll):
        columns = ["Algorithm", 'NumberOfHerds', 'CamHerdRatio']
        kcovChartBuilder.compare(columns, herdNumbers, simRatios, precision=1)
        columns = ["Algorithm", 'CamHerdRatio', 'NumberOfHerds']
        kcovChartBuilder.compare(columns, simRatios, herdNumbers)

    
    """""""""""""""""""""""""""
        kcoverage comparison alternative
    """""""""""""""""""""""""""

    if(generateAll and dataIncludeClusteringDistance):
        dataKcovsMean2 = dataMean.mean('Seed').mean('NumberOfHerds') #todo
        dataKcovsStd2 = dataMean.std('Seed').mean('NumberOfHerds')
        kcovChartBuilder2 = kcovlib.KcovChartBuilder(charts_dir, dataKcovsMean2, dataKcovsStd2, algos, kcovLabels)

        clusteringDistances = datasets[main_experiment].coords['ClusteringDistance'].data.tolist()
        clusteringDistances.reverse()
        
        
        columns = ["Algorithm", 'ClusteringDistance', 'CamHerdRatio'] #first fixed the variable values
        kcovChartBuilder2.compare(columns, clusteringDistances, simRatios, precision=1)
        columns = ["Algorithm", 'CamHerdRatio', 'ClusteringDistance']
        kcovChartBuilder2.compare(columns, simRatios, clusteringDistances)



    
    """""""""""""""""""""""""""
        LaTeX table
    """""""""""""""""""""""""""
#     import textwrap
#     selKcov = '2-coverage'
#     selherdNumbers = [2.0]
# #     selRatios = [0.2, 0.6, 1, 1.2, 1.6, 2]
#     selRatios = [ 0.5, 1.0, 1.5, 2.0]
#     txt = r'''
#     \begin{table}
#         \centering
#         \tiny
#         \begin{tabular}{lccccccc}%{lcccccccccccccccccccccccc}

#         \toprule
#         \multirow{2}{*}{$r$} & \multirow{2}{*}{\textsc{Approach}} 
#         & \multicolumn{6}{c}{\textsc{Ratio} $n/m$}\\
#         \cline{3-8}
#         & & ''' + '&'.join(['{:.1f}'.format(r) for r in selRatios]) + r'\\'
#     for herdNumber in selherdNumbers:
#         txt += "\n\n        " + r'\midrule \multirow{8}{*}{' + str(herdNumber) + "}\n"
#         for algo in algos:
#             txt += "        & " + algo.replace('_', r'\_') + ' '
#             for ratio in selRatios:
#                 txt += '& {:.2f}'.format(dataKcovsMean[selKcov].sel(Algorithm=algo, NumberOfHerds=herdNumber, CamHerdRatio=ratio).values.tolist())
#                 txt += ' ({:.2f}'.format(dataKcovsStd[selKcov].sel(Algorithm=algo, NumberOfHerds=herdNumber, CamHerdRatio=ratio).values.tolist()) + ') '
#             txt += r'\\' + "\n"
#     txt += r'''
#         \bottomrule
#         \end{tabular}
#         \caption{Comparison of mean $OMC_k$ achieved by different approaches with 
#         different communications ranges $r$ and different ratios for 
#         objects/cameras, standard deviation is indicated in brackets.}
#         \label{tab:results}
#     \end{table}
#     '''
#     txt = textwrap.dedent(txt.strip())
#     with open(charts_dir + 'KCov_latex.txt', 'w') as f:
#         f.write(txt)


    
    """""""""""""""""""""""""""
        distance traveled
    """""""""""""""""""""""""""
    # chartDataMean = dataDistMean.sel(CamHerdRatio=1, method='nearest')
    # chartDataStd = dataDistStd.sel(CamHerdRatio=1, method='nearest')
    
    # simRatio = 1
    # for r,herdNumber in enumerate(herdNumbers):
    #     fig = plt.figure(figsize=(6,6))
    #     ax = fig.add_subplot(1, 1, 1)
    #     ax.set_ylim([0,1])
    #     #if j<size:
    #     #ax.set_title("n/m = {0:.1f}".format(simRatio))
    #     if j%cols == 0:
    #         ax.set_ylabel("MovEfficiency (%)")
    #     plt.xticks(rotation=35, ha='right')
    #     ax.yaxis.grid(True)

    #     #for i,s in enumerate(kcovVariables):
    #     values = [chartDataMean.MovEfficiency.sel(Algorithm=algoname, NumberOfHerds=herdNumber).values.tolist() for algoname in algos]
    #     errors = [chartDataStd.MovEfficiency.sel(Algorithm=algoname, NumberOfHerds=herdNumber).values.tolist() for algoname in algos]
    #     ax.bar(algos, values, yerr=errors, capsize=4, color=kcovColors[i], ecolor=kcovEcolors[i])

    #     plt.tight_layout()
    #     fig.savefig(charts_dir + 'MovEfficiency_CamHerdRatio-'+str(simRatio)+'_herdNumber-'+str(herdNumber)+'.pdf')
    #     plt.close(fig)
    
    
    
    
    
    
        
# %%
