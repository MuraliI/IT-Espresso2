def envMapping = [    
    'test': [
        'cloud': [marathonUrl: 'http://10.16.7.225:8080', registry: 'rhldcmesboot711.na.rccl.com:5000'],
        'lab': [marathonUrl: 'http://10.135.10.153:8080', registry: 'rhldcmesboot721.na.rccl.com:5000']
    ],
    'stage': [
        'cloud': [marathonUrl: 'http://10.16.7.225:8080', registry: 'rhldcmesboot711.na.rccl.com:5000'],
        'lab': [marathonUrl: 'http://10.135.10.153:8080', registry: 'rhldcmesboot721.na.rccl.com:5000']
    ]
]

def keys = envMapping['test'].keySet() as String[]
print keys