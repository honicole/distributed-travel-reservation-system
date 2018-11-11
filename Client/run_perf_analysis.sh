if [[ "$#" -ne 3 ]]; then
    echo "# Usage: ./run_perf_analysis.sh [<middleware_hostname> [<middleware_port>]] option(0-2)(SCSR-SCMR-MCMR)"
    exit 1
fi

java -Djava.security.policy==java.policy -cp ../bin/middleware:../bin Client.PerformanceAnalyzer $1 $2 $3
