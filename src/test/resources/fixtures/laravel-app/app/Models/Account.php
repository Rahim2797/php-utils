<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Account extends Model
{
    protected $fillable = [
        'name',
        'plan',
        'is_test',
    ];

    public bool $runtimeOnlyFlag = false;

    protected function casts(): array
    {
        return [
            'is_test' => 'bool',
        ];
    }
}
